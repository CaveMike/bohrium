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
from publication import Publication

SUBSCRIPTION_KEYS = ('topic', 'dev_id', 'pub_id', 'user_id', 'created', 'modified', 'revision', 'key')

class Subscription(ndb.Model):
    ROOT_KEY = ndb.Key('Subscription', 'subscriptions')

    # HTML formatting.
    KEYS = SUBSCRIPTION_KEYS
    KEYS_WRITABLE = SUBSCRIPTION_KEYS[:2]
    KEYS_READONLY = SUBSCRIPTION_KEYS[2:]
    ROWS = {}
    COLUMNS = {}

    topic = ndb.StringProperty(required=True, validator=Fields.validate_not_empty) # FIXME: remove after debugging.
    dev_id = ndb.StringProperty(required=True, validator=Fields.validate_dev_id)
    pub_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        # Look up publication.
        topic = Fields.get(kv, 'topic')
        logging.getLogger().debug('topic=' + str(topic))
        if not topic:
            return

        obj.topic = topic # FIXME: remove after debugging.

        publication_keys = Publication.query(Publication.topic == topic, ancestor=Publication.ROOT_KEY).fetch(keys_only=True)
        logging.getLogger().debug('publication_keys (' + str(len(publication_keys)) + ')=' + str(publication_keys))
        if not publication_keys:
            return

        obj.dev_id = Fields.get(kv, 'dev_id')
        obj.pub_id = publication_keys[0].urlsafe()
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
            return '/subscription/' + self.get_id() + '/'
        else:
            return '/subscription/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/subscription/'
        child_url = str(parent_url) + '<id:' + Fields.REGEX_URLSAFE + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=SubscriptionsHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=SubscriptionsHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=SubscriptionsHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=SubscriptionHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=SubscriptionHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=SubscriptionHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=SubscriptionsHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=SubscriptionsHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=SubscriptionHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=SubscriptionHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class SubscriptionsHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(SubscriptionsHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Subscription), codec=CodecJson())

class SubscriptionHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(SubscriptionHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Subscription), codec=CodecJson())

class SubscriptionsHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(SubscriptionsHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Subscription), codec=CodecHtml(Subscription))

class SubscriptionHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(SubscriptionHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Subscription), codec=CodecHtml(Subscription))
