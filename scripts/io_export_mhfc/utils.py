# Logging: easier workflow with some utility methods to log and handle errors
import struct
import os
import bpy
import sys

class ErrorError(RuntimeError):
	"""When something happened that can't conform with the specification.
	Aka: the user's fault
	"""
	pass

class FatalError(RuntimeError):
	"""When something happened that really shouldn't happen.
	Aka: my fault
	"""
	pass

class ReportedError(RuntimeError):
	"""Thrown when a Reporter fails to run. That is an error or a fatal exception occured during
	running it.
	"""
	warnings = None
	infos = None
	error = (None, None, None) # Error tuple as from sys.exc_info()
	_reporter = None

	def throw(self):
		"""Throws itself, resulting in a correct traceback
		"""
		raise self from self.error[1] # from exc_value

	def is_reporter(self, candidate):
		return self._reporter is None or self._reporter is candidate

	@classmethod
	def from_exception(cls, reporter, exc = None):
		"""Constructs a ReportedError from the current exception handling context.
		"""
		if exc is None:
			exc = sys.exc_info()
		exc_type, exc_value, traceback = exc
		if isinstance(exc_value, ReportedError):
			exc_value._reporter = reporter
			return exc_value # DONE
		report = cls("An error occured:\n" + str(exc_value))
		report._reporter = reporter
		report.warnings = []
		report.infos = []
		report.error = exc
		return report

def static_access(func):
	"""Provides static access to member functions by calling the function with self set to None
	"""
	import functools
	class Functor(object):
		def __get__(self, instance, owner):
			# DON'T CHECK FOR instance is None, unlike functions, which then return themselves
			return functools.partial(func, instance)
	return Functor()

class Reporter(object):
	"""Via this class one can make reports of a process. That is return warnings, errors and
	fatal exceptions
	"""
	_stack = []

	_warnings = None
	_infos = None
	_error = (None, None, None)
	_passexceptions = None
	_engaged = None

	def __init__(self, catchexceptions=True):
		"""@param catchexceptions: if True when exiting the context because of an exception occured,
		it is not passed on, but instead an error is reported.
		Note that this does not change how an ReportedError is handled. They are reported if they
		belong to this reporter.
		"""
		self._warnings = []
		self._infos = []
		self._passexceptions = not catchexceptions
		self._error = (None, None, None)
		self._engaged = False

	def __enter__(self):
		if self._error[1] is not None:
			raise RuntimeError("Clear the reporter with .get_report() before reusing it")
		self._engaged = True
		Reporter._stack.append(self)
		return self

	def __exit__(self, exc_type, exc_value, traceback):
		try:
			exc = exc_type, exc_value, traceback
			if exc_value is None:
				# Completed normally, yay
				return False
			if isinstance(exc_value, ReportedError):
				# Allows for nesting of multiple reporters
				if exc_value.is_reporter(self):
					assert(self._error[1] is None)
					self._warnings.extend(exc_value.warnings)
					self._infos.extend(exc_value.infos)
					self._error = exc_value.error
					return True # Catch it, was ours
				else:
					exc_value.warnings.extend(self._warnings)
					exc_value.infos.extend(self._infos)
					return False # Pass it on, to another reporter
			if self._passexceptions:
				return False # Pass it on, don't handle it
			self._error = exc
		finally:
			self._engaged = False
			assert(Reporter._stack.pop() is self)

	@static_access
	def warning(self, message, *args, **wargs):
		"""When something happened that can be recovered from but isn't
		conformant never-the-less
		"""
		if self is None:
			if not Reporter._stack:
				return #Swallow it
			self = Reporter._stack[-1]
		formatted = message.format(*args, **wargs)
		self._warnings.append(formatted)

	@static_access
	def info(self, message, *args, **wargs):
		"""A useful information for the user
		"""
		if self is None:
			if not Reporter._stack:
				return #Swallow it
			self = Reporter._stack[-1]
		formatted = message.format(*args, **wargs)
		self._infos.append(formatted)

	@static_access
	def error(self, message, *args, cause=None, **wargs):
		"""When something happened that can't conform with the specification.
		Aka: the user's fault
		"""
		if self is not None and not self._engaged:
			raise RuntimeError("Can't file an error without __enter__'ing this Reporter")
		formatted = message.format(*args, **wargs)
		try:
			raise ErrorError(formatted) from cause
		except ErrorError:
			exc = ReportedError.from_exception(self)
		exc.throw()

	@static_access
	def fatal(self, message, *args, cause=None, **wargs):
		"""
		When something happened that really shouldn't happen.
		Aka: my fault
		"""
		if self is not None and not self._engaged:
			raise RuntimeError("Can't file an error without __enter__'ing this Reporter")
		formatted = message.format(*args, **wargs)
		message = "This should not have happened. Report to WorldSEnder:\n{mess}".format(mess=formatted)
		try:
			raise FatalException(message) from cause
		except FatalException:
			exc = ReportedError.from_exception(self)
		exc.throw()

	def print_report(self, op):
		# Print the things
		for info in self._infos:
			op.report({'INFO'}, str(info))
		for warning in self._warnings:
			op.report({'WARNING'}, str(warning))
		if not self.was_success():
			op.report({'ERROR'}, str(self._error[1]))

	def was_success(self):
		return self._error[1] is None

def extract_safe(collection, item, mess_on_fail, *args, on_fail = Reporter.error, **wargs):
	"""Ensures that the item is in the collection by reporting an error with
	the specified message if not.
	Calls on_fail when it fails to extract the element with the formatted message and
	the keyword argument 'cause' set to the KeyError that caused it to fail
	"""
	try:
		return collection[item]
	except KeyError as e:
		on_fail(mess_on_fail.format(*args, coll=collection, item=item, **wargs), cause = e)

def write_string(string, file_h):
	"""Writes a String to a file
	"""
	file_h.write(string.encode("utf-8") + b'\x00')

def write_packed(fmt, file_h, *args):
	"""Packs the given data into the given bytebuffer using the given format
	"""
	file_h.write(struct.pack(fmt, *args))

def to_valid_loc(assetstr):
	'''Replaces all non Java Characters with '_' to form a valid package/class name
	@see also http://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-IdentifierChars
	'''
	## TODO: replace all nonconforming characters with '_' (UNDERSCORE)
	#assetstr = '_'.join(re.split(r'[[\x00-\x40\x5b-\x60]--[{pathsep}]]'.format(pathsep=os.path.sep), assetstr))
	#if re.match(r'^[0-9]') is not None:
	#	assetstr = '_'+assetstr
	#return assetstr
	## ^ that requires Regex Set Operations
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
			Reporter.error("Asset-String {loc}: Splitted string mustn't be empty".format(loc=assetstr))
		return "assets/{mod}/{file}".format(mod=vals[0], file=vals[1])
	else:
		Reporter.error("Asset-String {loc} can't contain more than one ':'".format(loc=assetstr))

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
