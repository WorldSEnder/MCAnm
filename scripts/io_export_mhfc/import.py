import .utils
import json
import zipfile

import bpy
import bmesh

def import_tabula4(model):
	raise NotImplemented


import_fns = {
	4: import_tabula4
}

def import_tabula(filepath, scene):
	with ZipFile(filepath, 'b') as tabula:
		modelstr = tabula.read('model.json').decode()
	model = json.loads(modelstr)
	# Successfully loaded the model json, now convert it
	version = model['projVersion']
	try:
		import_fns[version](model)
	except (KeyError, NotImplementedError) as e:
		raise NotImplementedError("tabula version {v} is not (yet) supported".format(version))
