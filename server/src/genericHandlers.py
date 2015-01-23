import logging

from webapp2 import RequestHandler

class GenericParentHandlerJson(RequestHandler):
    def __init__(self, request, response, adapter, codec):
        super(GenericParentHandlerJson, self).__init__(request, response)
        self.adapter = adapter
        self.codec = codec

    # Create child
    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        kv = self.codec.decode(self.request)
        if not kv:
            self.abort(500, detail='Decoding error')

        obj = self.adapter.create(kv=kv)
        if not obj:
            self.abort(500, detail='Create error')

        result = self.codec.encode(self.request, obj)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Read all
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        objs = self.adapter.read_all()
        if not objs:
            self.abort(500, detail='Read error')

        result = self.codec.encode(self.request, objs)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Update all
    def put(self, *args, **kwargs):
        logging.getLogger().debug('put: args: %s, kwargs: %s' % (args, kwargs))

        objs = self.adapter.update_all()
        if not objs:
            self.abort(500, detail='Update error')

        result = self.codec.encode(self.request, objs)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Delete all
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        self.adapter.delete_all()

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write('')

class GenericHandlerJson(RequestHandler):
    def __init__(self, request, response, adapter, codec):
        super(GenericHandlerJson, self).__init__(request, response)
        self.adapter = adapter
        self.codec = codec

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

        result = self.codec.encode(self.request, obj)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Read one
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.read_one(id)
        if not obj:
            self.abort(404)

        result = self.codec.encode(self.request, obj)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Update one
    def put(self, *args, **kwargs):
        logging.getLogger().debug('put: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)

        kv = self.codec.decode(self.request)
        if not kv:
            self.abort(500, detail='Decoding error')

        obj = self.adapter.update_one(id, kv=kv)
        if not obj:
            self.abort(404)

        result = self.codec.encode(self.request, obj)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

    # Delete one
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)
        obj = self.adapter.delete_one(id)
        if not obj:
            self.abort(404)

        result = self.codec.encode(self.request, obj)
        if not result:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(result)

class GenericParentHandlerHtml(RequestHandler):
    def __init__(self, request, response, adapter, codec):
        super(GenericParentHandlerHtml, self).__init__(request, response)
        self.adapter = adapter
        self.codec = codec

    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        method = self.request.get('method')
        if method == 'delete':
            self.delete(*args, **kwargs)
        else:
            self._post(*args, **kwargs)

    # Create
    def _post(self, *args, **kwargs):
        logging.getLogger().debug('_post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        kv = self.codec.decode(self.request)
        if not kv:
            self.abort(500, 'Decode error')

        kv.update(kwargs)

        obj = self.adapter.create(kv=kv)
        if not obj:
            self.abort(500, detail='Create error')

        self.redirect(self.codec.redirect_url())

    # Read all
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        objs = self.adapter.read_all()
        # objs may be None

        results = self.codec.encode(self.request, objs, 'html/all.html')
        if not results:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(results)

    # Delete all
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        self.adapter.delete_all()

        self.redirect(self.codec.redirect_url())

class GenericHandlerHtml(RequestHandler):
    def __init__(self, request, response, adapter, codec):
        super(GenericHandlerHtml, self).__init__(request, response)
        self.adapter = adapter
        self.codec = codec

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

    def post(self, *args, **kwargs):
        logging.getLogger().debug('post: args: %s, kwargs: %s' % (args, kwargs))

        method = self.request.get('method')
        if method == 'delete':
            self.delete(*args, **kwargs)
        else:
            self._post(*args, **kwargs)

    # Read one
    def get(self, *args, **kwargs):
        logging.getLogger().debug('get: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)

        obj = self.adapter.read_one(id)
        if not obj:
            self.abort(404)

        results = self.codec.encode(self.request, obj, 'html/one.html')
        if not results:
            self.abort(500, detail='Encoding error')

        self.response.headers['Content-Type'] = self.codec.content_type
        self.response.write(results)

    # Update one
    def _post(self, *args, **kwargs):
        logging.getLogger().debug('_post: args: %s, kwargs: %s' % (args, kwargs))

        if not self.request.body:
            self.abort(400)

        id = self.get_id(kwargs, self.request)

        kv = self.codec.decode(self.request)
        if not kv:
            self.abort(500, 'Decode error')

        kv.update(kwargs)

        obj = self.adapter.update_one(id, kv=kv)
        if not obj:
            self.abort(500, 'Update error')

        self.redirect(self.codec.redirect_url())

    # Delete one
    def delete(self, *args, **kwargs):
        logging.getLogger().debug('delete: args: %s, kwargs: %s' % (args, kwargs))

        id = self.get_id(kwargs, self.request)

        obj = self.adapter.delete_one(id)
        if not obj:
            self.abort(404)

        self.redirect(self.codec.redirect_url())
