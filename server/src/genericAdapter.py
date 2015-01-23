import logging

from google.appengine.api import users
from google.appengine.ext import ndb

class GenericAdapter(object):
    def __init__(self, cls, allowDuplicates=False, createIfMissing=True, updateIfExists=True):
        self.cls = cls
        self.allowDuplicates = allowDuplicates
        self.createIfMissing = createIfMissing
        self.updateIfExists = updateIfExists

    # Create
    def create(self, request, body, parent=None):
        if not parent:
            parent=self.cls.ROOT_KEY

        obj = self.cls(parent=parent)
        self.cls.load(obj, request, body)

        keys = self.cls.query_by_id(obj.get_id())
        if keys and not self.allowDuplicates:
            if self.updateIfExists:
                logging.getLogger().debug('update existing object for create')
                obj = self.update_one(obj.get_id(), request, body)
                return obj
            else:
                logging.getLogger().error('duplicate object')
                return None

        obj.put()
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
    def read_all(self, request, body=None, parent=None):
        if not parent:
            parent=self.cls.ROOT_KEY

        return self.cls.query(ancestor=parent).order(-self.cls.modified).fetch()

    def read_one(self, id, request, body=None):
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
    def update_all(self, request, body=None):
        logging.getLogger().error('not implemented')
        return None

    def update_one(self, id, request, body):
        keys = self.cls.query_by_id(id)
        if not keys:
            if self.createIfMissing:
                logging.getLogger().error('creating missing object for update')
                return self.create(request, body)
            else:
                return None

        if len(keys) > 1:
            logging.getLogger().error('too many objects found')
            return None

        obj = keys[0].get()
        if not obj:
            logging.getLogger().error('object not read')
            return None

        self.cls.load(obj, request, body)

        obj.revision += 1
        obj.put()
        return obj

    # Delete
    def delete_all(self, request, body=None, parent=None):
        if not parent:
            parent=self.cls.ROOT_KEY

        keys = self.cls.query(ancestor=parent).fetch(keys_only=True)
        if not keys:
            return None

        ndb.delete_multi(keys)
        return keys

    def delete_one(self, id, request, body=None):
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

    def parse_template_values(self, request):
        template_values = {}

        if users.get_current_user():
            url = users.create_logout_url('/')
            url_linktext = 'Logout'
        else:
            url = users.create_login_url(request.uri)
            url_linktext = 'Login'

        template_values['url'] = url
        template_values['url_linktext'] = url_linktext

        template_values['keys'] = self.cls.KEYS

        template_values['writable'] = self.cls.KEYS_WRITABLE
        """
        if users.is_current_user_admin():
            template_values['writable'] = self.cls.KEYS
        else:
            template_values['writable'] = self.cls.KEYS_WRITABLE
        """

        template_values['readonly'] = self.cls.KEYS_READONLY
        template_values['rows'] = self.cls.ROWS
        template_values['columns'] = self.cls.COLUMNS

        return template_values

    def redirect_url(self):
        return self.cls.url_name()
