# Logging: easier workflow with some utility methods to log and handle errors
import struct
import os
import bpy

class FatalException(RuntimeError):
	pass

class ErrorException(RuntimeError):
	pass

def fatal(message, *args, cause=None, **wargs):
	"""
	When something happened that really shouldn't happen.
	Aka: my fault
	"""
	formatted = message.format(*args, **wargs)
	raise FatalException("This should not have happened. Report to WorldSEnder:\n{mess}".format(mess=formatted)) from cause

def error(message, *args, cause=None, **wargs):
	"""
	When something happened that can't conform with the specification.
	Aka: the user's fault
	"""
	formatted = message.format(*args, **wargs)
	raise ErrorException(formatted) from cause

def warning(message, *args, **wargs):
	"""
	When something happened that can be recovered from but isn't
	conformant never-the-less
	"""
	formatted = message.format(*args, **wargs)
	warning.active_op.report({'WARNING'}, formatted)

def extract_safe(collection, item, mess_on_fail, *args, **wargs):
	"""
	Ensures that the item is in the collection by raising
	a fatal error with the specified message if not
	"""
	try:
		return collection[item]
	except KeyError:
		error(mess_on_fail, *args, coll=collection, item=item, **wargs)

def write_string(string, file_h):
	"""
	Writes a String to a file
	"""
	file_h.write(string.encode("utf-8") + b'\x00')

def write_packed(fmt, file_h, *args):
	"""
	Packs the given data into the given bytebuffer using the given format
	"""
	file_h.write(struct.pack(fmt, *args))

def to_valid_loc(assetstr):
	'''
	Replaces all non Java Characters with '_' to form a valid package/class name
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
	"""
	Translates and minecraft asset string to a filesystem path
	"""
	if not assetstr:
		error("Asset-String can't be empty")
	vals = assetstr.split(':')
	if len(vals) == 1:
		return "assets/minecraft/" + assetstr
	elif len(vals) == 2:
		if not vals[0] or not vals[1]:
			error("Asset-String {loc}: Splitted string mustn't be empty".format(loc=assetstr))
		return "assets/{mod}/{file}".format(mod=vals[0], file=vals[1])
	else:
		error("Asset-String {loc} can't contain more than one ':'".format(loc=assetstr))

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
