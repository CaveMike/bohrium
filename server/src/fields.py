import hashlib
import json
import logging
import re

class Fields(object):
    USER_ID_SALT = 'tvpd$wo8'

    REGEX_URLSAFE = '[a-zA-Z0-9_\-]+'
    REGEX_USER_ID = '[a-f0-9]+'
    REGEX_DEV_ID = '[a-f0-9]+'
    REGEX_REG_ID = '[a-zA-Z0-9_\-]+'
    REGEX_CONFIG_ID = '[a-zA-Z0-9_\-]+'

    @staticmethod
    def sanitize_user_id(value):
        value = value.strip()
        value = hashlib.sha512(Fields.USER_ID_SALT + value).hexdigest()
        return value

    @staticmethod
    def validate_not_empty(prop, value):
        if not value:
            raise TypeError('expected a non-empty string, for %s' % repr(prop))

        return value.strip()

    @staticmethod
    def validate_email(prop, value):
        result = re.match('^\s*([A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6})\s*$', value, flags=re.IGNORECASE)
        if not result:
            raise TypeError('expected an email address, got %s' % repr(value))

        return result.group(0)

    @staticmethod
    def validate_config_id(prop, value):
        result = re.match('^\s*(' + Fields.REGEX_CONFIG_ID + ')\s*$', value, flags=re.IGNORECASE)
        if not result:
            raise TypeError('expected a config_id, got %s' % repr(value))

        return result.group(0)

    @staticmethod
    def validate_reg_id(prop, value):
        result = re.match('^\s*(' + Fields.REGEX_REG_ID + ')\s*$', value, flags=re.IGNORECASE)
        if not result:
            raise TypeError('expected a reg_id, got %s' % repr(value))

        return result.group(0)

    @staticmethod
    def validate_dev_id(prop, value):
        result = re.match('^\s*(' + Fields.REGEX_DEV_ID + ')\s*$', value, flags=re.IGNORECASE)
        if not result:
            raise TypeError('expected a dev_id, got %s' % repr(value))

        return result.group(0)

    @staticmethod
    def validate_user_id(prop, value):
        result = re.match('^\s*(' + Fields.REGEX_USER_ID + ')\s*$', value, flags=re.IGNORECASE)
        if not result:
            raise TypeError('expected a user_id, got %s' % repr(value))

        return result.group(0)

    @staticmethod
    def get(kv, key, default=None):
        try:
            value = kv[key]
        except KeyError:
            value = None

        if value:
            return value
        else:
            return default
