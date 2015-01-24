import datetime
import json
import logging

from google.appengine.api import users
from google.appengine.ext import ndb

class NdbJsonEncoder(json.JSONEncoder):
    def default(self, o):
        # If this is a key, grab the actual model.
        if isinstance(o, ndb.Key):
            o = o.get()

        if isinstance(o, ndb.Model):
            return o.to_dict()
        elif isinstance(o, users.User):
            return o.email()
        elif isinstance(o, (datetime.datetime, datetime.date, datetime.time)):
            return str(o)
        return json.JSONEncoder.default(self, o)

class CodecJson(object):
    def __init__(self, content_type='application/json'):
        super(CodecJson, self).__init__()
        self.content_type = content_type

    def encode(self, request, obj):
        return json.dumps(obj=obj, cls=NdbJsonEncoder)

    def decode(self, request):
        if not request.body:
            return None

        try:
            j = json.loads(request.body)
        except ValueError:
            return None

        logging.getLogger().debug('json=' + str(j) )
        return j
