import json
import logging

from ndbjsonencoder import NdbJsonEncoder

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
