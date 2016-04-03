# Logging: easier workflow with some utility methods to log and handle errors
from collections import defaultdict, namedtuple
from enum import Enum
import bpy
import os
import struct
import sys


class LogLevel(Enum):
    DEBUG = 'debug'
    INFO = 'info'
    WARNING = 'warning'
    ERROR = 'error'
    FATAL = 'fatal'

    def is_fatal(self):
        return self == LogLevel.ERROR or self == LogLevel.FATAL

    def get_bl_report_level(self):
        if self == LogLevel.DEBUG:
            return {'DEBUG'}
        if self == LogLevel.INFO:
            return {'INFO'}
        if self == LogLevel.WARNING:
            return {'WARNING'}
        if self == LogLevel.ERROR:
            return {'ERROR'}
        if self == LogLevel.FATAL:
            return {'ERROR'}

ReportItem = namedtuple('ReportItem', 'message cause')


class Report(object):
    _reports = None

    def __init__(self):
        self._reports = defaultdict(list)

    def extend(self, other):
        """Append another report to this one
        """
        for level in other._reports:
            self._reports[level].extend(other._reports[level])

    def append(self, message, level=LogLevel.INFO, cause=None):
        if cause is None:
            cause = sys.exc_info()
        self._reports[level].append(ReportItem(message, cause))
        return self

    def get_items(self, level):
        return self._reports[level]

    def contains_fatal(self):
        for level in self._reports:
            if level.is_fatal() and self._reports[level]:
                return True
        return False

    def print_report(self, op):
        for level in self._reports:
            op_level = level.get_bl_report_level()
            for item in self.get_items[level]:
                op.report(op_level, str(item.message))


class ReportedError(RuntimeError):
    """Thrown when a Reporter fails to run. That is an error or a fatal exception occured during
    running it.
    """
    report = None
    _target = None

    def __init__(self, message, target=None):
        super(ReportedError, self).__init__(message)
        self.report = Report()
        self._target = target

    def is_aimed_at(self, candidate):
        return self._target is None or self._target is candidate

    @classmethod
    def throw_from_exception(cls, reporter, level=LogLevel.ERROR, exc=None):
        """Constructs a ReportedError from the current exception handling context.
        """
        if exc is None:
            exc = sys.exc_info()
        exc_type, exc_value, traceback = exc
        message = "An error occured: " + str(exc_value)
        reported = cls(message, target=reporter)
        reported.report.append(exc, message, level=level, cause=exc)
        raise reported from exc_value


def static_access(func):
    """Provides static access to member functions by calling the function with self set to None
    """
    import functools

    class Functor(object):

        def __get__(self, instance, owner):
            # DON'T CHECK FOR instance is None, unlike functions, which then
            # return themselves
            return functools.partial(func, instance)
    return Functor()


class Reporter(object):
    """Via this class one can make reports of a process. That is return warnings, errors and
    fatal exceptions
    """
    _stack = []

    _report = None
    _caught = None
    _engaged = None
    _bl_op = None

    def __init__(self, caught_types=(Exception,), reported_to=None):
        """@param caught_types: A repeatable-iterable containing classinfos that will be used to check if an exception of type exc_t
        should be caught or not. A caught exception will be logged as LogLevel.ERROR and not passed onwards. Note that
        each entry of caught_types can be either a class or a tuple of classes and will be checked via issubclass(exc_t, entry).

        Note that this does not change how an ReportedError is handled. They are reported if they
        belong to this reporter.
        """
        self._report = Report()
        self._caught = caught_types
        self._engaged = False
        self._bl_op = reported_to
        # The following will check that the given caught_types is indeed legal
        # by performing a dummy check
        self._should_catch(type(None))

    def _should_catch(self, exc_type):
        return any(issubclass(exc_type, ct) for ct in self._caught)

    def __enter__(self):
        if self._engaged:
            raise RuntimeError("No multi-entry into a reporter allowed")
        self._engaged = True
        Reporter._stack.append(self)
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        try:
            exc = (exc_type, exc_value, traceback)
            if exc_value is None:
                # Completed normally, yay
                return False
            if isinstance(exc_value, ReportedError):
                # Allows for nesting of multiple reporters
                if exc_value.is_aimed_at(self):
                    self._report.extend(exc_value.report)
                    return True  # Catch it, was ours
                else:
                    exc_value.report.extend(self._report)
                    return False  # Pass it on, to another reporter
            if self._should_catch(exc_type):
                self._report.append(exc_value, level=LogLevel.ERROR, cause=exc)
                return True
            return False
        finally:
            self._engaged = False
            if self._bl_op is not None:
                self.print_report(self._bl_op)
            assert(Reporter._stack.pop() is self)

    def rebind_bl_op(self, op):
        """Binds a Blender op that will be reported to when this Reporter __exit__s
        """
        self._bl_op = op

    @classmethod
    def _get_reporter(cls, proposed):
        if proposed is not None:
            return proposed
        if not cls._stack:
            return None
        return cls._stack[-1]

    @static_access
    def warning(self, message, *args, **wargs):
        """When something happened that can be recovered from but isn't
        conformant never-the-less
        """
        self = Reporter._get_reporter(self)
        if self is None:
            return
        formatted = message.format(*args, **wargs)
        self._report.append(formatted, level=LogLevel.WARNING)

    @static_access
    def info(self, message, *args, **wargs):
        """A useful information for the user
        """
        self = Reporter._get_reporter(self)
        if self is None:
            return
        formatted = message.format(*args, **wargs)
        self._report.append(formatted, level=LogLevel.INFO)

    @static_access
    def debug(self, message, *args, **wargs):
        """Debug output, only output during debug mode
        """
        self = Reporter._get_reporter(self)
        if self is None:
            return
        formatted = message.format(*args, **wargs)
        self._report.append(formatted, level=LogLevel.DEBUG)

    @static_access
    def error(self, message, *args, cause=None, **wargs):
        """When something happened that can't conform with the specification.
        Aka: the user's fault
        """
        if self is not None and not self._engaged:
            raise RuntimeError(
                "Can't file an error without __enter__'ing this Reporter")
        formatted = message.format(*args, **wargs)
        try:
            raise ErrorError(formatted) from cause
        except ErrorError:
            ReportedError.rethrow_from_exception(self)

    @static_access
    def fatal(self, message, *args, cause=None, **wargs):
        """
        When something happened that really shouldn't happen.
        Aka: my fault
        """
        if self is not None and not self._engaged:
            raise RuntimeError(
                "Can't file an error without __enter__'ing this Reporter")
        formatted = message.format(*args, **wargs)
        message = "This should not have happened. Report to WorldSEnder:\n{mess}".format(
            mess=formatted)
        try:
            raise FatalException(message) from cause
        except FatalException:
            ReportedError.rethrow_from_exception(self)

    def print_report(self, op):
        self._report.print_report(op)

    def was_success(self):
        return not self._report.contains_fatal()


def extract_safe(collection, key, mess_on_fail, *args, on_fail=Reporter.error, **wargs):
    """Ensures that the item is in the collection by reporting an error with
    the specified message if not.
    Calls on_fail when it fails to extract the element with the formatted message and
    the keyword argument 'cause' set to the KeyError that caused it to fail

    @param collection: the collection to search in
    @param key: the key to search for
    @param mess_on_fail: a message that will get formatted and handed to on_fail
    @param on_fail: called when the key is not found in the collection as on_fail(formatted_message, cause=e)
    where e is the KeyError thrown by the collection. The result of this function is returned instead
    @param args: formatting arguments
    @param wargs: additional formatting keyword-arguments. Can not be 'coll' or 'item', those will be
    provided by default as the collection and the searched key

    @returns the item in the collection for the specified key or the result of on_fail if a KeyError is
    raised by collection[key]
    """
    try:
        return collection[key]
    except KeyError as e:
        return on_fail(mess_on_fail.format(*args, coll=collection, item=key, **wargs), cause=e)


def to_valid_loc(assetstr):
    '''Replaces all non Java Characters with '_' to form a valid package/class name
    @see also http://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-IdentifierChars
    '''
    # TODO: replace all nonconforming characters with '_' (UNDERSCORE)
    #assetstr = '_'.join(re.split(r'[[\x00-\x40\x5b-\x60]--[{pathsep}]]'.format(pathsep=os.path.sep), assetstr))
    # if re.match(r'^[0-9]') is not None:
    #	assetstr = '_'+assetstr
    # return assetstr
    # ^ that requires Regex Set Operations
    return assetstr.replace(' ', '_')


def asset_to_dir(assetstr):
    """Translates and minecraft asset string to a filesystem path. If the path is non conformant, an
    error is reported
    """
    if not assetstr:
        Reporter.error("Asset-String can't be empty")
    vals = assetstr.split(':')
    if len(vals) == 1:
        return "assets/minecraft/" + assetstr
    elif len(vals) == 2:
        if not vals[0] or not vals[1]:
            Reporter.error(
                "Asset-String {loc}: Splitted string mustn't be empty".format(loc=assetstr))
        return "assets/{mod}/{file}".format(mod=vals[0], file=vals[1])
    else:
        Reporter.error(
            "Asset-String {loc} can't contain more than one ':'".format(loc=assetstr))


def openw_save(filepath, flags, *args, **wargs):
    """
    Ensures that the directory for the filepath exists and creates it if
    necessary. Returns a file_handle to the open stream by calling
    open(filepath, flags, *args, **wargs)
    """
    filepath = bpy.path.abspath(filepath)
    dir = os.path.dirname(filepath)
    if not os.path.exists(dir):
        os.makedirs(dir)
    return open(filepath, flags, *args, **wargs)
