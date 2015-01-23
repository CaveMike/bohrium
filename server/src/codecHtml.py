import jinja
import logging

from google.appengine.api import users

class CodecHtml(object):
    def __init__(self, cls, content_type='text/html', showAdminFields=False):
        super(CodecHtml, self).__init__()
        self.cls = cls
        self.content_type = content_type
        self.showAdminFields = showAdminFields

    def encode(self, request, obj, template_name):
        template_values = {}

        # Add generic template values.
        if users.get_current_user():
            template_values['url'] = users.create_logout_url('/')
            template_values['url_linktext'] = 'Logout'
        else:
            template_values['url'] = users.create_login_url(request.uri)
            template_values['url_linktext'] = 'Login'

        template_values['result'] = obj

        # Add cls template values.
        template_values['keys'] = self.cls.KEYS
        template_values['writable'] = self.cls.KEYS_WRITABLE
        template_values['readonly'] = self.cls.KEYS_READONLY
        template_values['rows'] = self.cls.ROWS
        template_values['columns'] = self.cls.COLUMNS

        if self.showAdminFields and users.is_current_user_admin():
            template_values['writable'] = self.cls.KEYS

        # Render
        template = jinja.get_template(template_name)
        if not template:
            return None

        result = template.render(template_values)
        return result

    def decode(self, request):
        kv = {}
        for key in request.arguments():
            value = request.get(key)
            kv[key] = value

        return kv

    def redirect_url(self, obj=None):
        if obj:
            return obj.get_link()
        else:
            return ''
