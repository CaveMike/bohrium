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

CONFIG_KEYS = ('name', 'gcm_api_key', 'user_id', 'created', 'modified', 'revision', 'key')

class Config(ndb.Model):
    ROOT_KEY = ndb.Key('Config', 'configs')

    # HTML formatting.
    KEYS = CONFIG_KEYS
    KEYS_WRITABLE = CONFIG_KEYS[:2]
    KEYS_READONLY = CONFIG_KEYS[2:]
    ROWS = {}
    COLUMNS = {}

    name = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    gcm_api_key = ndb.StringProperty(default='', required=True)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    @staticmethod
    def load(obj, kv):
        logging.getLogger().debug('kv=' + str(kv))

        obj.name = Fields.get(kv, 'name', 'default')
        obj.gcm_api_key = Fields.get(kv, 'gcm_api_key')
        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    @staticmethod
    def query_by_id(id):
        return Config.query(Config.name == id, ancestor=Config.ROOT_KEY).order(-Config.modified).fetch(keys_only=True)

    def get_id(self):
        return self.name

    def get_link(self, includeId=True):
        if includeId:
            return '/config/' + self.get_id() + '/'
        else:
            return '/config/'

    @classmethod
    def get_master_db(cls, id='active'):
        keys = cls.query_by_id(id)
        if not keys:
            logging.getLogger().warn('no objects found')
            return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        obj = keys[0].get()
        if not obj:
            logging.getLogger().error('object not read')
            return None

        return obj

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/config/'
        child_url = str(parent_url) + '<id:' + Fields.REGEX_CONFIG_ID + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=ConfigsHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=ConfigsHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=ConfigsHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=ConfigHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=ConfigHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=ConfigHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=ConfigsHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=ConfigsHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=ConfigHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=ConfigHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),
        ]

class ConfigsHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(ConfigsHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Config), codec=CodecJson())

class ConfigHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(ConfigHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Config), codec=CodecJson())

class ConfigsHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(ConfigsHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Config), codec=CodecHtml(Config))

class ConfigHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(ConfigHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Config), codec=CodecHtml(Config))


Config.get_master_db()
