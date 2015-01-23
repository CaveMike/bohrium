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
from publication import Publication
from subscription import Subscription

PMESSAGE_KEYS = ('publication_key', 'message', 'user_id', 'created', 'modified', 'revision', 'key')

class PMessage(ndb.Model):
    ROOT_KEY = ndb.Key('PMessage', 'pmessages')

    # HTML formatting.
    KEYS = PMESSAGE_KEYS
    KEYS_WRITABLE = PMESSAGE_KEYS[:2]
    KEYS_READONLY = PMESSAGE_KEYS[2:]
    ROWS = {}
    COLUMNS = {}

    pub_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    message = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        # Look up publication.
        obj.pub_id = Fields.get(kv, 'pub_id')
        obj.message = Fields.get(kv, 'message', 'debug-message')
        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    def send(self):
        publication_key = ndb.Key(urlsafe=self.pub_id)
        publication = publication_key.get()
        logging.getLogger().debug('publication=' + str(publication))
        logging.getLogger().debug('topic=' + str(publication.topic))

        subscription_keys = Subscription.query(Subscription.pub_id == self.pub_id, ancestor=Subscription.ROOT_KEY).fetch(keys_only=True)
        logging.getLogger().debug('subscription_keys (' + str(len(subscription_keys)) + ')=' + str(subscription_keys))
        if not subscription_keys:
            return

        dev_ids = [subscription_key.get().dev_id for subscription_key in subscription_keys]
        logging.getLogger().debug('dev_ids (' + str(len(dev_ids)) + ')=' + str(dev_ids))
        if not dev_ids:
            return

        data = {'from-user-id' : self.user_id, 'type' : 'publish-message', 'context' : publication.topic, 'message' : self.message}
        logging.getLogger().debug('data=' + str(data))

        gcm.send(data=data, reg_ids=[], dev_ids=dev_ids, user_ids=[])

    @staticmethod
    def query_by_id(id):
        return ( ndb.Key(urlsafe=id), )

    def get_id(self):
        if not self.key.id():
            return None

        return self.key.urlsafe()

    def get_link(self, includeId=True):
        if includeId:
            return '/publication/' + str(self.pub_id) + '/message/' + str(self.key.urlsafe()) + '/'
        else:
            return '/publication/' + str(self.pub_id) + '/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/publication/<pub_id:' + Fields.REGEX_URLSAFE + '>/message/'
        child_url = str(parent_url) + '/<id:' + Fields.REGEX_URLSAFE + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=PMessagesHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=PMessagesHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=PMessagesHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=PMessageHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=PMessageHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=PMessageHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=PMessagesHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=PMessagesHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=PMessageHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=PMessageHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class PMessageAdapter(GenericAdapter):
    def create_child(self, request, body=None):
        obj = super(PMessageAdapter, self).create_child(request, body)
        if obj:
            obj.send()

        return obj

class PMessagesHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(PMessagesHandlerJson, self).__init__(request, response, adapter=PMessageAdapter(PMessage), codec=CodecJson())

class PMessageHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(PMessageHandlerJson, self).__init__(request, response, adapter=PMessageAdapter(PMessage), codec=CodecJson())

class PMessagesHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(PMessagesHandlerHtml, self).__init__(request, response, adapter=PMessageAdapter(PMessage), codec=CodecHtml(PMessage))

class PMessageHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(PMessageHandlerHtml, self).__init__(request, response, adapter=PMessageAdapter(PMessage), codec=CodecHtml(PMessage))
