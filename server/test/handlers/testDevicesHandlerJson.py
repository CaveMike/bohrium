import datetime
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
from device import DevicesHandlerJson
from fields import Fields
from helpers import setCurrentUser
from testdata import TestData

class DevicesHandlerJsonTest(unittest.TestCase):
    def setUp(self):
        # Create a WSGI application.
        app = webapp2.WSGIApplication([('/', DevicesHandlerJson)])

        # Wrap the app with WebTest's TestApp.
        self.testapp = webtest.TestApp(app)
        self.testbed = testbed.Testbed()
        self.testbed.activate()
        self.testbed.init_datastore_v3_stub()
        self.testbed.init_user_stub()
        ndb.get_context().clear_cache()

        setCurrentUser(email=Deploy.GAE_ADMIN, user_id='1234', is_admin=True)
        user_id=Fields.sanitize_user_id(users.get_current_user().user_id())

        test = TestData.TEST_DEVICES[0]
        self.device = Device(parent=Device.ROOT_KEY, user_id=user_id, name=test['name'], reg_id=test['reg_id'], dev_id=test['dev_id'], resource=test['resource'], type=test['type'])
        self.key = self.device.put()

    def tearDown(self):
        self.testbed.deactivate()

    def testDevicesHandlerGet(self):
        response = self.testapp.get('/')
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, 'application/json')
        self.assertTrue(response.body)
        j = json.loads(response.body)
        self.assertEqual(1, len(j))
        d = j[0]
        self.assertEqual(self.device.name, d['name'])
        self.assertEqual(self.device.dev_id, d['dev_id'])
        self.assertEqual(self.device.reg_id, d['reg_id'])
        self.assertEqual(self.device.resource, d['resource'])
        self.assertEqual(self.device.type, d['type'])
        self.assertEqual(self.device.user_id, d['user_id'])
        self.assertEqual(str(self.device.created), d['created'])
        self.assertEqual(str(self.device.modified), d['modified'])
        self.assertEqual(self.device.revision, d['revision'])

    def testDevicesHandlerPost(self):
        params = json.dumps(obj=self.device, cls=NdbJsonEncoder)
        response = self.testapp.post('/', content_type='application/json', params=params)
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
        self.assertTrue(str(self.device.created), d['created'])
        self.assertTrue(str(self.device.modified), d['modified'])
        self.assertEqual(self.device.revision, d['revision'])

    def testDevicesHandlerPost400(self):
        # POST with no data.
        self.assertRaises(webtest.app.AppError, self.testapp.post, url='/')

    def testDevicesHandlerPut500(self):
        # PUT not implemented.
        self.assertRaises(webtest.app.AppError, self.testapp.put, url='/')

    def testDevicesHandlerDelete(self):
        response = self.testapp.delete('/')
        self.assertEqual(response.status_int, 200)
        self.assertEqual(response.content_type, 'application/json')
        self.assertFalse(response.body)
