import hashlib
import json
import logging
import re

from google.appengine.api import users
from google.appengine.ext import ndb

from webapp2 import RequestHandler

from codecHtml import CodecHtml
from codecJson import CodecJson
from codecXml import CodecXml
from codecYaml import CodecYaml
from contentRoute import ContentRoute
from fields import Fields
from genericHandlers import GenericParentHandlerJson
from genericHandlers import GenericHandlerJson
from genericHandlers import GenericParentHandlerHtml
from genericHandlers import GenericHandlerHtml
from genericAdapter import GenericAdapter

DEVICE_KEYS = ('name', 'resource', 'type', 'dev_id', 'reg_id', 'user_id', 'created', 'modified', 'revision', 'key')

class Device(ndb.Model):
    ROOT_KEY = ndb.Key('Device', 'devices')

    # HTML formatting.
    KEYS = DEVICE_KEYS
    KEYS_WRITABLE = DEVICE_KEYS[:5]
    KEYS_READONLY = DEVICE_KEYS[5:]
    ROWS = {}
    COLUMNS = {}

    name = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    dev_id = ndb.StringProperty(required=True, validator=Fields.validate_dev_id)
    reg_id = ndb.StringProperty(required=True, validator=Fields.validate_reg_id)
    resource = ndb.StringProperty(required=False)
    type = ndb.StringProperty(default='ac2dm', required=True)
    user_id = ndb.StringProperty(required=True, validator=Fields.validate_not_empty)
    created = ndb.DateTimeProperty(auto_now_add=True, required=True)
    modified = ndb.DateTimeProperty(auto_now=True, required=True)
    revision = ndb.IntegerProperty(default=0, required=True)

    #staticmethod
    def load(obj, kv=None):
        logging.getLogger().debug('kv=' + str(kv))

        obj.name = Fields.get(kv, 'name', users.get_current_user().nickname())
        obj.dev_id = Fields.get(kv, 'dev_id', '0123456789abcdef')
        obj.reg_id = Fields.get(kv, 'reg_id', '0123abcd')
        obj.resource = Fields.get(kv, 'resource', 'debug-resource')
        obj.type = Fields.get(kv, 'type', 'debug-type')

        obj.user_id = Fields.sanitize_user_id(users.get_current_user().user_id())

        logging.getLogger().debug('obj=' + str(obj))

    @staticmethod
    def query_by_id(id):
        return Device.query(Device.dev_id == id, ancestor=Device.ROOT_KEY).order(-Device.modified).fetch(keys_only=True)

    def get_id(self):
        return self.dev_id

    def get_link(self, includeId=True):
        if includeId:
            return '/device/' + self.get_id() + '/'
        else:
            return '/device/'

    @staticmethod
    def get_routes(base_url=''):
        parent_url = str(base_url) + '/device/'
        child_url = str(parent_url) + '<id:' + Fields.REGEX_DEV_ID + '>/'

        return [ \
        # JSON (parent)
        ContentRoute(template=parent_url, handler=DevicesHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=DevicesHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=DevicesHandlerJson,
            methods=('DELETE')),

        # JSON (child)
        ContentRoute(template=child_url, handler=DeviceHandlerJson,
            header='Accept', header_values=('application/json',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=DeviceHandlerJson,
            header='Content-Type', header_values=('application/json',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=DeviceHandlerJson,
            methods=('DELETE')),

        # HTML (parent)
        ContentRoute(template=parent_url, handler=DevicesHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=DevicesHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST')),

        # HTML (child)
        ContentRoute(template=child_url, handler=DeviceHandlerHtml,
            header='Accept', header_values=('text/html',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=DeviceHandlerHtml,
            #header='Content-Type', header_values=('application/x-www-form-urlencoded',),
            methods=('POST',)),

        # XML (parent)
        ContentRoute(template=parent_url, handler=DevicesHandlerXml,
            header='Accept', header_values=('application/xml',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=DevicesHandlerXml,
            header='Content-Type', header_values=('application/xml',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=DevicesHandlerXml,
            header='Content-Type', header_values=('application/xml',),
            methods=('DELETE')),

        # XML (child)
        ContentRoute(template=child_url, handler=DeviceHandlerXml,
            header='Accept', header_values=('application/xml',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=DeviceHandlerXml,
            header='Content-Type', header_values=('application/xml',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=DeviceHandlerXml,
            header='Content-Type', header_values=('application/xml',),
            methods=('DELETE')),

        # XML (parent)
        ContentRoute(template=parent_url, handler=DevicesHandlerYaml,
            header='Accept', header_values=('application/x-yaml',),
            methods=('GET',)),
        ContentRoute(template=parent_url, handler=DevicesHandlerYaml,
            header='Content-Type', header_values=('application/x-yaml',),
            methods=('POST', 'PUT')),
        ContentRoute(template=parent_url, handler=DevicesHandlerYaml,
            header='Content-Type', header_values=('application/x-yaml',),
            methods=('DELETE')),

        # XML (child)
        ContentRoute(template=child_url, handler=DeviceHandlerYaml,
            header='Accept', header_values=('application/x-yaml',),
            methods=('GET',)),
        ContentRoute(template=child_url, handler=DeviceHandlerYaml,
            header='Content-Type', header_values=('application/x-yaml',),
            methods=('POST', 'PUT')),
        ContentRoute(template=child_url, handler=DeviceHandlerYaml,
            header='Content-Type', header_values=('application/x-yaml',),
            methods=('DELETE')),
        ]

class DevicesHandlerJson(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(DevicesHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecJson())

class DeviceHandlerJson(GenericHandlerJson):
    def __init__(self, request, response):
        super(DeviceHandlerJson, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecJson())

class DevicesHandlerHtml(GenericParentHandlerHtml):
    def __init__(self, request, response):
        super(DevicesHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecHtml(Device))

class DeviceHandlerHtml(GenericHandlerHtml):
    def __init__(self, request, response):
        super(DeviceHandlerHtml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecHtml(Device))

class DevicesHandlerXml(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(DevicesHandlerXml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecXml())

class DeviceHandlerXml(GenericHandlerJson):
    def __init__(self, request, response):
        super(DeviceHandlerXml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecXml())

class DevicesHandlerYaml(GenericParentHandlerJson):
    def __init__(self, request, response):
        super(DevicesHandlerYaml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecYaml())

class DeviceHandlerYaml(GenericHandlerJson):
    def __init__(self, request, response):
        super(DeviceHandlerYaml, self).__init__(request, response, adapter=GenericAdapter(Device), codec=CodecYaml())
