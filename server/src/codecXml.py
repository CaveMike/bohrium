import datetime
import logging

import xml.etree.ElementTree as ET

from google.appengine.api import users
from google.appengine.ext import ndb

from xml.dom import minidom
from xml.parsers.expat import ExpatError

class CodecXml(object):
    def __init__(self, content_type='application/xml'):
        super(CodecXml, self).__init__()
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

    def encode(self, request, obj):
        if isinstance(obj, ndb.Model):
            obj = (obj, )

        root = ET.Element('root')
        for o in obj:
            e = ET.SubElement(root, o.get_link().strip('/').replace('/', '-'))
            for key, value in o.to_dict().items():
                e.set(key, self.encodeAttribute(value))

        try:
            return self.prettify(root)
        except ExpatError:
            return tostring(root)

    def decode(self, request):
        if not request.body:
            return None

        objs = []

        try:
            print(request.body)
            tree = ET.ElementTree(ET.fromstring(request.body))
            print(tree)
            root = tree.getroot()
            print(root)

            for element in root:
                print(element)
                kv = {}
                for attribute in element.keys():
                    print(attribute)
                    kv[attribute] = element.get(attribute)
                objs.append(kv)
        except ValueError:
            return None

        logging.getLogger().debug('xml=' + str(objs) )

        if len(objs) == 1:
            return objs[0]
        else:
            return objs

    def prettify(self, elem):
        rough_string = ET.tostring(elem, 'utf-8')
        reparsed = minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent='  ')