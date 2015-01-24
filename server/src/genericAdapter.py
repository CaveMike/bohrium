import logging

from google.appengine.ext import ndb
from google.appengine.ext.db import BadValueError

class GenericAdapter(object):
    def __init__(self, cls, allowDuplicates=False, createIfMissing=True, updateIfExists=True):
        self.cls = cls
        self.allowDuplicates = allowDuplicates
        self.createIfMissing = createIfMissing
        self.updateIfExists = updateIfExists

    # Create
    def create(self, kv, parent=None):
        logging.getLogger().debug('create: kv: %s, parent: %s' % (kv, parent))
        if not parent:
            parent=self.cls.ROOT_KEY

        obj = self.cls(parent=parent)
        self.cls.load(obj, kv)

        id = obj.get_id()
        if id:
            keys = self.cls.query_by_id(id)
            if keys and not self.allowDuplicates:
                if self.updateIfExists:
                    logging.getLogger().debug('update existing object for create')
                    obj = self.update_one(obj.get_id(), kv)
                    return obj
                else:
                    logging.getLogger().error('duplicate object')
                    return None

        try:
            obj.put()
        except BadValueError:
            return None

        return obj

    def create_child(self, id, request, body=None):
        keys = self.cls.query_by_id(id)
        if not keys:
            logging.getLogger().error('no objects found')
            return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        key = keys[0]
        return create(request, body, parent=keys)

    # Read
    def read_all(self, parent=None):
        if not parent:
            parent=self.cls.ROOT_KEY

        return self.cls.query(ancestor=parent).order(-self.cls.modified).fetch()

    def read_one(self, id):
        keys = self.cls.query_by_id(id)
        if not keys:
            logging.getLogger().error('no objects found')
            return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        obj = keys[0].get()
        if not obj:
            logging.getLogger().error('object not read')
            return None

        return obj

    # Update
    def update_all(self):
        logging.getLogger().error('not implemented')
        return None

    def update_one(self, id, kv=None):
        keys = self.cls.query_by_id(id)
        if not keys:
            if self.createIfMissing:
                logging.getLogger().error('creating missing object for update')
                return self.create(kv)
            else:
                return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        obj = keys[0].get()
        if not obj:
            logging.getLogger().error('object not read')
            return None

        self.cls.load(obj, kv)

        obj.revision += 1
        obj.put()
        return obj

    # Delete
    def delete_all(self, parent=None):
        if not parent:
            parent=self.cls.ROOT_KEY

        keys = self.cls.query(ancestor=parent).fetch(keys_only=True)
        if not keys:
            return None

        ndb.delete_multi(keys)
        return keys

    def delete_one(self, id):
        keys = self.cls.query_by_id(id)
        if not keys:
            logging.getLogger().error('objects not found')
            return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        obj = keys[0].get()

        keys[0].delete()
        return obj
