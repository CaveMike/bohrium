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

PUBLICATION_KEYS = ('topic', 'description', 'user_id', 'created', 'modified', 'revision', 'key')

class Publication(ndb.Model):
    ROOT_KEY = ndb.Key('Publication', 'publications')

    # HTML formatting.
    KEYS = PUBLICATION_KEYS
    KEYS_WRITABLE = PUBLICATION_KEYS[:2]
    KEYS_READONLY = PUBLICATION_KEYS[2:]
    ROWS = {}
    COLUMNS = {}

    topic = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    description = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        obj.topic = Fields.get(kv, 'topic', 'debug-topic')
        obj.description = Fields.get(kv, 'description', 'debug-description')
        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    @staticmethod
    def query_by_id(id):
        return ( ndb.Key(urlsafe=id), )

    def get_id(self):
        if not self.key.id():
            return None

        return self.key.urlsafe()

    def get_link(self, includeId=True):
        if includeId:
            return '/publication/' + self.get_id() + '/'
        else:
            return '/publication/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/publication/'
        child_url = str(parent_url) + '<id:' + Fields.REGEX_URLSAFE + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=PublicationsHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=PublicationsHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=PublicationsHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=PublicationHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=PublicationHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=PublicationHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=PublicationsHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=PublicationsHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=PublicationHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=PublicationHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class PublicationsHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(PublicationsHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Publication), codec=CodecJson())

class PublicationHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(PublicationHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Publication), codec=CodecJson())

class PublicationsHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(PublicationsHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Publication), codec=CodecHtml(Publication))

class PublicationHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(PublicationHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Publication), codec=CodecHtml(Publication))
