import jinja
import json
import logging

from webapp2 import RequestHandler

from ndbjsonencoder import NdbJsonEncoder

class GenericParentHandlerJson(RequestHandler):
    def __init__(self, request, response, adapter):
        super(GenericParentHandlerJson, self).__init__(request, response)
        self.adapter = adapter
        logging.getLogger().debug('adapter=' + str(type(self.adapter)))

    # Create child
    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        obj = self.adapter.create(self.request, self.request.body)
        if not obj:
            self.abort(500, detail='Create error')

        result = json.dumps(obj=obj, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Read all
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        objs = self.adapter.read_all(self.request, self.request.body)
        if not objs:
            self.abort(500, detail='Read error')

        result = json.dumps(obj=objs, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Update all
    def put(self, *args, **kwargs):
        logging.getLogger().debug('put: args: %s, kwargs: %s' % (args, kwargs))

        objs = self.adapter.update_all(self.request, self.request.body)
        if not objs:
            self.abort(500, detail='Update error')

        result = json.dumps(obj=objs, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Delete all
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        self.adapter.delete_all(self.request, self.request.body)

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write('')

class GenericHandlerJson(RequestHandler):
    def __init__(self, request, response, adapter):
        super(GenericHandlerJson, self).__init__(request, response)
        self.adapter = adapter

    @staticmethod
    def get_id(kwargs, request):
        PARAM_KEY_NAME = 'id'

        if kwargs.has_key(PARAM_KEY_NAME):
            id = kwargs[PARAM_KEY_NAME]
            logging.getLogger().debug('from kwargs: id: %s' % (id))
        else:
            id = request.get(PARAM_KEY_NAME)
            logging.getLogger().debug('from request: id: %s' % (id))

        if not id:
            self.abort(500, detail='ID parameter not found.')

        logging.getLogger().debug('id: %s' % (id))
        return id

    # Create child
    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.create_child(id, self.request, self.request.body)
        if not obj:
            self.abort(404)

        result = json.dumps(obj=obj, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Read one
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.read_one(id, self.request, self.request.body)
        if not obj:
            self.abort(404)

        result = json.dumps(obj=obj, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Update one
    def put(self, *args, **kwargs):
        logging.getLogger().debug('put: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.update_one(id, self.request, self.request.body)
        if not obj:
            self.abort(404)

        result = json.dumps(obj=obj, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

    # Delete one
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.delete_one(id, self.request, self.request.body)
        if not obj:
            self.abort(404)

        result = json.dumps(obj=obj, cls=NdbJsonEncoder)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result)

class GenericParentHandlerHtml(RequestHandler):
    def __init__(self, request, response, adapter):
        super(GenericParentHandlerHtml, self).__init__(request, response)
        self.adapter = adapter

    # List all
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        template_values = self.adapter.parse_template_values(self.request)
        template_values['results'] = self.adapter.read_all(self.request)

        template = jinja.get_template('html/all.html')

        result = template.render(template_values)

        self.response.headers['Content-Type'] = 'text/html'
        self.response.write(result)

    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        method = self.request.get('method')
        if method == 'delete':
            self.delete(*args, **kwargs)
        else:
            self._post(*args, **kwargs)

    # Create child
    def _post(self, *args, **kwargs):
        logging.getLogger().debug('_post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        obj = self.adapter.create(self.request, body=None)
        if obj:
            self.redirect(obj.redirect_url())

        self.response.headers['Content-Type'] = 'text/html'
        self.response.write('error, not created')

    # Delete all
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        result = self.adapter.delete_all(self.request)

        self.redirect(self.adapter.redirect_url())

class GenericHandlerHtml(RequestHandler):
    def __init__(self, request, response, adapter):
        super(GenericHandlerHtml, self).__init__(request, response)
        self.adapter = adapter

    @staticmethod
    def get_id(kwargs, request):
        PARAM_KEY_NAME = 'id'

        if kwargs.has_key(PARAM_KEY_NAME):
            id = kwargs[PARAM_KEY_NAME]
            logging.getLogger().debug('from kwargs: id: %s' % (id))
        else:
            id = request.get(PARAM_KEY_NAME)
            logging.getLogger().debug('from request: id: %s' % (id))

        if id:
            logging.getLogger().debug('id: %s' % (id))
            return id

        self.abort(501)

    # List one
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)

        template_values = self.adapter.parse_template_values(self.request)
        template_values['result'] = self.adapter.read_one(id, self.request)

        template = jinja.get_template('html/one.html')

        result = template.render(template_values)

        self.response.write(result)

    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        method = self.request.get('method')
        if method == 'delete':
            self.delete(*args, **kwargs)
        else:
            self._post(*args, **kwargs)

    # Update one
    def _post(self, *args, **kwargs):
        logging.getLogger().debug('_post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        id = self.get_id(kwargs, self.request)
        result = self.adapter.update_one(id, self.request, body=None)

        self.redirect(self.adapter.redirect_url())

    # Delete one
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        result = self.adapter.delete_one(id, self.request)

        self.redirect(self.adapter.redirect_url())
