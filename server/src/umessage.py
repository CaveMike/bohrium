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
from gcmHelpers import gcm
from genericHandlers import GenericParentHandlerJson
from genericHandlers import GenericHandlerJson
from genericHandlers import GenericParentHandlerHtml
from genericHandlers import GenericHandlerHtml
from genericAdapter import GenericAdapter

UMESSAGE_KEYS = ('to_user_id', 'message', 'user_id', 'created', 'modified', 'revision', 'key')

class UMessage(ndb.Model):
    ROOT_KEY = ndb.Key('UMessage', 'umessages')

    # HTML formatting.
    KEYS = UMESSAGE_KEYS
    KEYS_WRITABLE = UMESSAGE_KEYS[:2]
    KEYS_READONLY = UMESSAGE_KEYS[2:]
    ROWS = {'message' : 80}
    COLUMNS = {'message' : 5}

    to_user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    message = ndb.StringProperty(required=True)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        obj.to_user_id = Fields.get(kv, 'to_user_id')
        obj.message = Fields.get(kv, 'message', 'debug-message')
        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    def send(self):
        data = {'from-user-id' : str(self.user_id), 'type' : 'user-message', 'context' : str(self.to_user_id), 'message' : self.message}
        logging.getLogger().debug('data=' + str(data))

        gcm.send(data=data, reg_ids=[], dev_ids=[], user_ids=[self.to_user_id])

    @staticmethod
    def query_by_id(id):
        return ( ndb.Key(urlsafe=id), )

    def get_id(self):
        if not self.key.id():
            return None

        return self.key.urlsafe()

    def get_link(self, includeId=True):
        if includeId:
            return '/user/' + str(self.user_id) + '/message/' + str(self.key.urlsafe()) + '/'
        else:
            return '/user/' + str(self.user_id) + '/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/user/<to_user_id:' + Fields.REGEX_USER_ID + '>/message/'
        child_url = str(parent_url) + '/<id:' + Fields.REGEX_URLSAFE + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=UMessagesHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=UMessagesHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=UMessagesHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=UMessageHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=UMessageHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=UMessageHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=UMessagesHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=UMessagesHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=UMessageHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=UMessageHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class UMessageAdapter(GenericAdapter):
    def create_child(self, request, body=None):
        obj = super(UMessageAdapter, self).create_child(request, body)
        if obj:
            obj.send()

        return obj

class UMessagesHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(UMessagesHandlerJson, self).__init__(request, response, adapter=UMessageAdapter(UMessage), codec=CodecJson())

class UMessageHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(UMessageHandlerJson, self).__init__(request, response, adapter=UMessageAdapter(UMessage), codec=CodecJson())

class UMessagesHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(UMessagesHandlerHtml, self).__init__(request, response, adapter=UMessageAdapter(UMessage), codec=CodecHtml(UMessage))

class UMessageHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(UMessageHandlerHtml, self).__init__(request, response, adapter=UMessageAdapter(UMessage), codec=CodecHtml(UMessage))
