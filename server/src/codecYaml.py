import datetime
import logging
import yaml

from google.appengine.api import users
from google.appengine.ext import ndb

class CodecYaml(object):
    def __init__(self, content_type='application/x-yaml'):
        super(CodecYaml, self).__init__()
        self.content_type = content_type

    def encodeAttribute(self, o):
        # If this is a key, grab the actual model.
        if isinstance(o, ndb.Key):
            o = o.get()

        if isinstance(o, ndb.Model):
            return o.to_dict()
        elif isinstance(o, users.User):
            return o.email()
        elif isinstance(o, (datetime.datetime, datetime.date, datetime.time)):
            return str(o)
        return str(o)

    def encode(self, request, objs):
        if isinstance(objs, ndb.Model):
            objs = (objs, )

        out = []
        for obj in objs:
            kv = {}
            for key, value in obj.to_dict().items():
                kv[key] = self.encodeAttribute(value)
            out.append(kv)

        return yaml.dump(out)

    def decode(self, request):
        if not request.body:
            return None

        try:
            y = yaml.load(request.body)
        except ValueError:
            return None

        logging.getLogger().debug('yaml=' + str(y) )
        return y
