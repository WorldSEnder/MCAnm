import json
import zipfile

import bpy
import bmesh

from .utils import Reporter
from mathutils import Matrix, Vector, Quaternion, Euler
from math import radians
from contextlib import ExitStack

def import_tabula4(model, animations_only):
	tex_width, tex_height = model['textureWidth'], model['textureHeight']
	with ExitStack() as stack:
		bm = bmesh.new()
		stack.callback(bm.free)
		
		def convert_uv(u, v):
			return u/tex_width, 1- v/tex_height
		def Scale(vec):
			s = Matrix.Identity(4)
			s.row[0][0], s.row[1][1], s.row[2][2] = scale
		def Rotation(rot):
			return Euler(rot, 'XYZ').to_matrix().to_4x4()
		def make_cube(cube, to_global, parent):
			# TODO: build the cube and convert to internal format
			identifier = cube['identifier']
			name = cube['name']

			position = Vector(cube['position']) # Rotation point
			offset = Vector(cube['offset']) # Offset
			scale = Scale(cube['scale'])
			rotation = Rotation(cube['rotation'])
			dimensions = Vector(cube['dimensions'])

			if identifier in identifier_to_cube:
				Reporter.warning("Identifier reused in the model: {id}", id=identifier)
			identifier_to_cube = cube
			local_to_global = to_global
			for child in cube['children']:
				make_cube(child, local_to_global, cube)
		def make_group(group, to_global, parentgroup):
			for cube in group['cubes']:
				make_cube(cube, to_global, None)
			for child in group['cubeGroups']:
				make_cube(child, to_global, group)
		def make_animation(*args):
			pass # TODO: import animations aswell?

		model_name = model['modelName']
		author = model['authorName']
		model_transform = Matrix.Scale(1/16, 4)
		model_transform *= Scale(model['scale'])
		identifier_to_cube = {}

		for group in model['cubeGroups']:
			make_group(group, model_transform, None)
		for cube in model['cubes']:
			make_cube(cube, model_transform, None)
		for animation in model['anims']:
			make_animation(animation, model_transform)

import_fns = {
	4: import_tabula4
}

def import_tabula(filepath, scene, animations_only):
	with ZipFile(filepath, 'b') as tabula:
		modelstr = tabula.read('model.json').decode()
	model = json.loads(modelstr)
	# Successfully loaded the model json, now convert it
	version = model['projVersion']
	try:
		import_fns[version](model, animations_only)
	except (KeyError, NotImplementedError) as e:
		Reporter.fatal("tabula version {v} is not (yet) supported".format(version))
