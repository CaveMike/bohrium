import json
import webapp2
import webtest
import unittest2 as unittest

from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.ext import testbed

from ndbjsonencoder import NdbJsonEncoder
from deploy import Deploy
from device import Device
from device import DeviceHandlerJson
from fields import Fields
from helpers import setCurrentUser
from testdata import TestData

class DeviceHandlerJsonTest(unittest.TestCase):
    def setUp(self):
        # Create a WSGI application.
        app = webapp2.WSGIApplication([ \
            webapp2.Route('/<id:.*>/', DeviceHandlerJson) \
        ])

        # Wrap the app with WebTest's TestApp.
        self.testapp = webtest.TestApp(app)
        self.testbed = testbed.Testbed()
        self.testbed.activate()
        self.testbed.init_datastore_v3_stub()
        self.testbed.init_user_stub()
        self.testbed.init_memcache_stub()
        ndb.get_context().clear_cache()

        setCurrentUser(email=Deploy.GAE_ADMIN, user_id='1234', is_admin=True)
        user_id=Fields.sanitize_user_id(users.get_current_user().user_id())

        test = TestData.TEST_DEVICES[0]
        self.device = Device(parent=Device.ROOT_KEY, user_id=user_id, name=test['name'], reg_id=test['reg_id'], dev_id=test['dev_id'], resource=test['resource'], type=test['type'])
        self.key = self.device.put()

    def tearDown(self):
        self.testbed.deactivate()

    def testDeviceHandlerJsonGetExisting200(self):
        headers = {
            'Accept' : 'application/json',
        }

        response = self.testapp.get(url='/' + self.device.dev_id + '/', headers=headers)
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, headers['Accept'])
        self.assertEqual(response.body, json.dumps(obj=self.device, cls=NdbJsonEncoder))
        self.assertTrue(response.body)
        j = json.loads(response.body)
        d = j
        self.assertEqual(self.device.name, d['name'])
        self.assertEqual(self.device.dev_id, d['dev_id'])
        self.assertEqual(self.device.reg_id, d['reg_id'])
        self.assertEqual(self.device.resource, d['resource'])
        self.assertEqual(self.device.type, d['type'])
        self.assertEqual(self.device.user_id, d['user_id'])

    def testDeviceHandlerJsonGetMissing404(self):
        headers = {
            'Accept' : 'application/json',
        }

        self.assertRaises(webtest.app.AppError, self.testapp.get, url='/1234/', headers=headers)

    def testDeviceHandlerJsonPostMissing501(self):
        # POST not implemented.
        self.assertRaises(webtest.app.AppError, self.testapp.post, url='/' + self.device.dev_id + '/')

    def testDeviceHandlerJsonPutExisting200(self):
        self.device.name = 'new name'
        self.device.reg_id = '0123456789abcdef'
        response = self.testapp.put(url='/' + self.device.dev_id + '/',
            content_type='application/json',
            params=json.dumps(obj=self.device, cls=NdbJsonEncoder))
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, 'application/json')
        self.assertTrue(response.body)
        j = json.loads(response.body)
        d = j
        self.assertEqual(self.device.name, d['name'])
        self.assertEqual(self.device.dev_id, d['dev_id'])
        self.assertEqual(self.device.reg_id, d['reg_id'])
        self.assertEqual(self.device.resource, d['resource'])
        self.assertEqual(self.device.type, d['type'])
        self.assertEqual(self.device.user_id, d['user_id'])
        self.assertEqual(self.device.revision, d['revision'])

    def testDeviceHandlerJsonPutMissing200(self):
        # Remove default test entry.
        self.key.delete()
        self.key = None

        response = self.testapp.put(url='/' + self.device.dev_id + '/',
            content_type='application/json',
            params=json.dumps(obj=self.device, cls=NdbJsonEncoder))
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, 'application/json')
        self.assertTrue(response.body)
        j = json.loads(response.body)
        d = j
        self.assertEqual(self.device.name, d['name'])
        self.assertEqual(self.device.dev_id, d['dev_id'])
        self.assertEqual(self.device.reg_id, d['reg_id'])
        self.assertEqual(self.device.resource, d['resource'])
        self.assertEqual(self.device.type, d['type'])
        self.assertEqual(self.device.user_id, d['user_id'])
        self.assertEqual(self.device.revision, d['revision'])

    def testDeviceHandlerJsonDeleteExisting200(self):
        response = self.testapp.delete(url='/' + self.device.dev_id + '/')
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, 'application/json')
        self.assertTrue(response.body)
        j = json.loads(response.body)
        d = j
        self.assertEqual(self.device.name, d['name'])
        self.assertEqual(self.device.dev_id, d['dev_id'])
        self.assertEqual(self.device.reg_id, d['reg_id'])
        self.assertEqual(self.device.resource, d['resource'])
        self.assertEqual(self.device.type, d['type'])
        self.assertEqual(self.device.user_id, d['user_id'])
        self.assertEqual(self.device.revision, d['revision'])

    def testDeviceHandlerJsonDeleteMissing404(self):
        self.assertRaises(webtest.app.AppError, self.testapp.delete, url='/1234/')

    # FIXME: this is a test for DMessage.
    def estDevicePublishHandlerJsonPost(self):
        p = { 'dev_id' : self.device.dev_id, 'message' : 'test' }
        response = self.testapp.post(url='/' + self.device.dev_id + '/publish/',
            content_type='application/json',
            params=json.dumps(obj=p, cls=NdbJsonEncoder))
        self.assertEqual(response.status_int, 200)
        self.assertTrue(response.body)
        self.assertTrue(False)
