import hashlib
import json
import logging
import re

from google.appengine.api import users
from google.appengine.ext import ndb

from webapp2 import RequestHandler

from codecHtml import CodecHtml
from codecJson import CodecJson
from contentRoute import ContentRoute
from fields import Fields
from genericHandlers import GenericParentHandlerJson
from genericHandlers import GenericHandlerJson
from genericHandlers import GenericParentHandlerHtml
from genericHandlers import GenericHandlerHtml
from genericAdapter import GenericAdapter

USER_KEYS = ('name', 'description', 'groups', 'email', 'user_id', 'created', 'modified', 'revision', 'key')

class User(ndb.Model):
    ROOT_KEY = ndb.Key('User', 'users')

    # HTML formatting.
    KEYS = USER_KEYS
    KEYS_WRITABLE = USER_KEYS[:3]
    KEYS_READONLY = USER_KEYS[3:]
    ROWS = {'description' : 80}
    COLUMNS = {}

    name = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    description = ndb.StringProperty(default='', required=True)
    groups = ndb.StringProperty(default='', required=True)
    email = ndb.StringProperty(required=True, validator=Fields.validate_email)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        obj.name = Fields.get(kv, 'name', users.get_current_user().nickname())
        obj.description = Fields.get(kv, 'description', 'debug-description')
        obj.groups = Fields.get(kv, 'groups', 'debug-groups')

        obj.email = users.get_current_user().email()
        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    @staticmethod
    def query_by_id(id):
        return User.query(User.user_id == id, ancestor=User.ROOT_KEY).order(-User.modified).fetch(keys_only=True)

    def get_id(self):
        return self.user_id

    def get_link(self, includeId=True):
        if includeId:
            return '/user/' + self.get_id() + '/'
        else:
            return '/user/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/user/'
        child_url = str(parent_url) + '<id:' + Fields.REGEX_USER_ID + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=UsersHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=UsersHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=UsersHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=UserHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=UserHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=UserHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=UsersHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=UsersHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=UserHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=UserHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class UsersHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(UsersHandlerJson, self).__init__(request, response, adapter=GenericAdapter(User), codec=CodecJson())

class UserHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(UserHandlerJson, self).__init__(request, response, adapter=GenericAdapter(User), codec=CodecJson())

class UsersHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(UsersHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(User), codec=CodecHtml(User))

class UserHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(UserHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(User), codec=CodecHtml(User))
