# ##### BEGIN GPL LICENSE BLOCK #####
#
#  This program is free software; you can redistribute it and/or
#  modify it under the terms of the GNU General Public License
#  as published by the Free Software Foundation; either version 2
#  of the License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#
# ##### END GPL LICENSE BLOCK #####

bl_info = {
	"name": "Import: Techne Models (.tcn)",
	"description": "Import techne models into Blender",
	"author": "Martin Molzer",
	"version": (0, 1),
	"blender": (2, 70, 0),
	"location": "File > Import > Techne Models (.tcn)",
	"warning": "This does not advanced shapes from TurboModelThingy yet",
	"wiki_url": "",
	"tracker_url": "",
	"category": "Import-Export",
	}

from bpy.props import BoolProperty, EnumProperty, StringProperty
from bpy.types import Operator
from bpy_extras.io_utils import ImportHelper
from math import pi
from mathutils import Matrix, Vector
from xml.etree import ElementTree
from zipfile import ZipFile
import bmesh
import bpy
import os



class FatalException(RuntimeError):
	pass

active_op = None

def fatal(message, *args, cause=None, **wargs):
	formatted = message.format(*args, **wargs)
	raise FatalException("[Tcn Importer] {text}".format(text=formatted)) from cause

def warning(message, *args, **wargs):
	formatted = message.format(*args, **wargs)
	warning.active_op.report({'WARNING'}, formatted)

def rotation_matrix_from_euler(rot_x, rot_y, rot_z):
	rot_mat = (Matrix.Rotation(rot_y/180*pi, 3, (0, 1, 0))*
			   Matrix.Rotation(rot_z/180*pi, 3, (0, 0, 1))*
			   Matrix.Rotation(rot_x/180*pi, 3, (1, 0, 0)))
	return rot_mat

class ElementFactory(object):
	def __init__(self, tcnfile, **wargs):
		self.registry = wargs
		self.tcn = tcnfile
	def __call__(self, xml):
		try:
			typ = self.registry[xml.tag]
		except KeyError:
			fatal("Unknown Element: {name}", name=xml.tag)
		else:
			try:
				return typ(xml, self.tcn)
			except Exception as ex:
				fatal("Failed to initialize the element {tag}", tag=xml.tag, cause=ex)

class Element(object):
	"""Each xml entry gets transformed to an Element"""
	def __init__(self, *args, **wargs):
		pass
	def apply(self):
		"""Applies the element and returns a suitable value"""
		raise NotImplementedError()
	def append(self, subelement):
		"""Appends the subelement to this."""
		raise NotImplementedError()

class Techne(Element):
	"""The abstract root element"""
	def _init__(self, xml, tcn):
		self.version = xml.attrib['Version']
		self.author = None
		self.models = None
		self.name = None
	def append(self, subelement, factory):
		tag = subelement.tag
		if tag == 'Author':
			author = subelement.text
			self.author = author
		elif tag == 'Models':
			self.models = []
			return self
		elif tag == 'Model':
			model = factory(subelement)
			self.models.append(model)
			return model
		elif tag == 'Name':
			name = subelement.text
			self.name = name
		elif tag not in ('DateCreated', 'Description', 'PreviewImage', 'ProjectName', 'ProjectType'):
			warning("Unknown tag ({tag}) in <Techne>", tag=tag)
	def apply(self):
		if self.models is None:
			fatal("Invalid model.xml: No 'Models' tag.")
		if self.author is None:
			warning("No author found, defaulting to WorldSEnder")
			self.author = "WorldSEnder"
		if len(self.models) == 1 and self.name:
			self.models[0].name = self.name
		objects = []
		for model in self.models:
			obj = model.apply()
			if obj is None:
				continue
			if hasattr(obj.data, 'mcprops'):
				props = obj.data.mcprops
				props.artist = self.author
			objects.append(obj)
		return objects

class Model(Element):
	"""A model in the techne file"""
	def __init__(self, xml, tcn):
		tex = xml.attrib['texture']
		extr_file = os.path.join(bpy.app.tempdir, tex)
		try:
			tcn.extract(tex, path=bpy.app.tempdir)
		except KeyError:
			warning("Missing texture {texname} (was one supplied?)", texname=tex)
			self.img = None
		else:
			self.img = bpy.data.images.load(extr_file)
			self.img.pack()
			self.img.use_alpha = True
			os.remove(extr_file)
		self.shapes = None
		self.obj_scale = None
		self.uv_scale = None
		self.name = None
	def append(self, subelement, factory):
		tag = subelement.tag
		if tag == 'Name':
			self.name = subelement.text
		elif tag == 'GlScale':
			mat = Matrix.Identity(3)
			mat[0][0], mat[1][1], mat[2][2] = map(float,subelement.text.split(','))
			self.obj_scale = mat
		elif tag == 'TextureSize':
			mat = Matrix.Identity(2)
			mat[0][0], mat[1][1] = map(lambda x: 1/int(x),subelement.text.split(','))
			self.uv_scale = mat
		elif tag == 'Geometry':
			self.shapes = []
			return self
		elif tag == 'Folder':
			return self
		elif tag in ('Shape', 'Piece'):
			piece = factory(subelement)
			self.shapes.append(piece)
			return piece
		elif tag not in ('BaseClass',):
			warning("Unknown tag ({tag}) in <Model>", tag=tag)
	def apply(self):
		if self.name is None:
			fatal("Missing nametag in model")
		if self.shapes is None:
			fatal("Missing shapes in model {name}", name=self.name)
		if self.obj_scale is None:
			warning("Missing GlScale in model {name}, assuming Identity", name=self.name)
		if self.uv_scale is None:
			warning("Missing texture-size in model {name}. Computing from texture anyway", name=self.name)
		elif self.img is not None and (self.uv_scale[0][0] != 1/self.img.size[0] or self.uv_scale[1][1] != 1/self.img.size[1]):
			warning("In Model {name}: Texture is ({x}, {y}) but .tcn file said ({x_xml}, {y_xml})",
				name=self.name, x=self.img.size[0], y=self.img.size[1], x_xml=self.uv_scale[0], y_xml=self.uv_scale[1])
		# make mesh
		mesh = bpy.data.meshes.new(self.name)
		bm = bmesh.new()
		uv = bm.loops.layers.uv.new('UVLayer')
		tex = bm.faces.layers.tex.new('UVLayer')
		mc_loop = None
		if hasattr(mesh, 'mcprops'):
			mc_loop = bm.faces.layers.int.new('MCRenderGroupIndex')
		for shape_nbr, shape in enumerate(self.shapes):
			# points is a list of lists of points (representing a list of faces)
			points, idx_uvs = shape.apply(True)
			bpoints = []
			for p in points:
				co = self.obj_scale * p[0]
				norm = self.obj_scale * p[1]
				co.y = 24 - co.y
				co = Matrix(((-1/16, 0, 0), (0, 0, -1/16), (0, 1/16, 0))) * co
				norm = Matrix(((-1, 0, 0), (0, 0, -1), (0, 1, 0))) * norm
				bp = bm.verts.new(co)
				bp.normal = norm
				bpoints.append(bp)
			for face in idx_uvs:
				bface = bm.faces.new([bpoints[l[0]] for l in face])
				for loop, (_, uvco) in zip(bface.loops, face):
					uvco = self.uv_scale * uvco
					uvco[1] = 1 - uvco[1]
					loop[uv].uv = uvco
				bface[tex].image = self.img
				if mc_loop is not None:
					bface[mc_loop] = shape_nbr + 1
		bm.verts.index_update()
		bm.faces.index_update()
		bm.to_mesh(mesh)
		# make obj
		obj = bpy.data.objects.new(self.name, mesh)
		if hasattr(mesh, 'mcprops'):
			props = mesh.mcprops
			props.name = self.name
			props.uv_layer = uv.name
			if self.img is not None:
				props.default_img = self.img.name
			for shape in self.shapes:
				group = props.render_groups.add()
				group.name = shape.name
				if self.img is not None:
					group.image = self.img.name
		return obj

class Piece(Element):
	def __init__(self, xml, tcn):
		assert(xml.attrib['type'] == "9ec93754-b48d-4c70-ae4e-84d81ae55396")
		self.position = None
		self.rotation = None
		self.sub_shapes = None
		self.name = xml.attrib['Name']
	def append(self, subelement, factory):
		tag = subelement.tag
		if tag == 'Position':
			self.position = Vector(map(float,subelement.text.split(',')))
		elif tag == 'Rotation':
			rot_x, rot_y, rot_z = map(float,subelement.text.split(','))
			self.rotation = rotation_matrix_from_euler(rot_x, rot_y, rot_z)
		elif tag == 'Folder':
			return self
		elif tag == 'Shape':
			if self.sub_shapes is None:
				self.sub_shapes = []
			shape = factory(subelement)
			self.sub_shapes.append(shape)
			return shape
	def apply(self, apply_position):
		if self.position is None:
			fatal("Missing Position-property in Piece {name}", name=self.name)
		if self.rotation is None:
			fatal("Missing Rotation-property in Piece {name}", name=self.name)
		if self.sub_shapes is None:
			fatal("Missing shapes in Piece {name}", name=self.name)
		ps, idxuvs = [], []
		rot = self.rotation
		pos = self.position
		for shape in self.sub_shapes:
			shape_ps, shape_idxuvs = shape.apply(False)
			idx_off = len(ps)
			ps += [(rot * p + pos, n) for p, n in shape_ps]
			idxuvs += [(i + idx_off, uv) for i, uv in shape_idxuvs]
		return ps, idxuvs

class Shape(Element):
	def __init__(self, xml, tcn):
		self.name = xml.attrib['name']
		self.is_mirrored = None
		self.offset = None
		self.position = None
		self.rotation = None
		self.size = None
		self.texture_offset = None
	def __new__(cls, xml, tcn):
		class Debug(Shape):
			def __init__(self, xml, tcn):
				super().__init__(xml, tcn)
			def template_shape(self, size):
				return [], []
		if cls is not Shape:
			return super(Shape, cls).__new__(cls)
		typ = xml.attrib['type']
		if typ == "d9e621f7-957f-4b77-b1ae-20dcd0da7751":
			return Cube(xml, tcn) # cube
		elif typ == "0900de04-664f-4789-8562-07ffe1043e90":
			warning("Instantiating placeholder instead of Cone")
			return Debug(xml, tcn) # cone
		elif typ == "b94b0064-e61c-4517-8f99-adb273f1b33e":
			warning("Instantiating placeholder instead of Cylinder")
			return Debug(xml, tcn) # cylinder
		elif typ == "e1957603-6c07-4a1e-9047-bb1f45e57cea":
			warning("Instantiating placeholder instead of Sphere")
			return Debug(xml, tcn) # sphere
		elif typ == "de81aa14-bd60-4228-8d8d-5238bcd3caaa":
			warning("Instantiating placeholder instead of TMT Cube")
			return Debug(xml, tcn) # tmt cube
	def append(self, subelement, factory):
		tag = subelement.tag
		if tag in ('Animation', 'AnimationAngles', 'AnimationType'):
			return self
		elif tag == 'IsMirrored':
			val = subelement.text
			self.is_mirrored = val == "True"
		elif tag == 'Offset':
			self.offset = Vector(map(float,subelement.text.split(',')))
		elif tag == 'Position':
			self.position = Vector(map(float,subelement.text.split(',')))
		elif tag == 'Rotation':
			rot_x, rot_y, rot_z = map(float,subelement.text.split(','))
			self.rotation = rotation_matrix_from_euler(rot_x, rot_y, rot_z)
		elif tag == 'Size':
			self.size = Vector(map(int,subelement.text.split(',')))
		elif tag == 'TextureOffset':
			self.texture_offset = Vector(map(int,subelement.text.split(',')))
	def apply(self, apply_position):
		"""
		Should return a list of points (coord, normal) and a list
		of indices with mixed in uv-coords like this:
		[(co0, norm0), ((), ()), ...], [((idx0, uv0), (), ...), ...]
		If apply_offset the offset should be already applied
		"""
		if self.is_mirrored is None:
			fatal("Missing IsMirrored-property in Shape {name}", name=self.name)
		if self.offset is None:
			fatal("Missing Offset-property in Shape {name}", name=self.name)
		if self.position is None:
			fatal("Missing Position-property in Shape {name}", name=self.name)
		if self.rotation is None:
			fatal("Missing Rotation-property in Shape {name}", name=self.name)
		if self.size is None:
			fatal("Missing Size-property in Shape {name}", name=self.name)
		if self.texture_offset is None:
			fatal("Missing TextureOffset-property in Shape {name}", name=self.name)
		points, idx_uvs = self.template_shape(self.size)
		off = self.offset
		rot = self.rotation
		pos = self.position
		points = [(p + off, n) for p, n in points]
		if apply_position:
			points = [(rot * p + pos, rot * n) for p, n in points]
		tex_off = self.texture_offset
		idx_uvs = [[(idx, uv + tex_off) for idx, uv in face] for face in idx_uvs]
		if self.is_mirrored:
			m_mat = Matrix.Scale(-1, 3, (1, 0, 0))
			idx_offset = len(points)
			points += [(m_mat * p, m_mat * n) for p, n in points]
			idx_uvs += [[(idx + idx_offset, uv) for idx, uv in face[::-1]] for face in idx_uvs]
		return points, idx_uvs
	def template_shape(self):
		"""
		Templates the shape at 0, 0, 0.
		"""
		raise NotImplementedError()

class Cube(Shape):
	def __init__(self, xml, tcn):
		super().__init__(xml, tcn)
	def template_shape(self, size):
		x, y, z = size
		return ([(Vector((x, y, 0)), Vector((0.333333,0.666667,-0.666667))),
				 (Vector((0, y, 0)), Vector((-0.816497,0.408248,-0.408248))),
				 (Vector((0, y, z)), Vector((-0.333333,0.666667,0.666667))),
				 (Vector((x, y, z)), Vector((0.816497,0.408248,0.408248))),
				 (Vector((x, 0, 0)), Vector((0.666667,-0.666667,-0.333333))),
				 (Vector((0, 0, 0)), Vector((-0.408248,-0.408248,-0.816497))),
				 (Vector((0, 0, z)), Vector((-0.666667,-0.666667,0.333333))),
				 (Vector((x, 0, z)), Vector((0.408248,-0.408248,0.816497)))],
				[((5, Vector((z, z))),
				  (1, Vector((z, z+y))),
				  (0, Vector((z+x, z+y))),
				  (4, Vector((z+x, z)))),
				 ((6, Vector((0, z))),
				  (2, Vector((0, z+y))),
				  (1, Vector((z, z+y))),
				  (5, Vector((z, z)))),
				 ((6, Vector((z, 0))),
				  (5, Vector((z, z))),
				  (4, Vector((z+x, z))),
				  (7, Vector((z+x, 0)))),
				 ((7, Vector((z+z+x, z))),
				  (3, Vector((z+z+x, z+y))),
				  (2, Vector((z+x+z+x, z+y))),
				  (6, Vector((z+x+z+x, z)))),
				 ((4, Vector((z+x, z))),
				  (0, Vector((z+x, z+y))),
				  (3, Vector((z+z+x, z+y))),
				  (7, Vector((z+z+x, z)))),
				 ((1, Vector((z+x+x, 0))),
				  (2, Vector((z+x+x, z))),
				  (3, Vector((z+x, z))),
				  (0, Vector((z+x, 0))))])

def read_tcn(context, file_p, op):
	with ZipFile(file_p, 'r') as tcnfile:
		factory = ElementFactory(tcnfile, Techne=Techne, Model=Model, Shape=Shape, Piece=Piece)
		scene = context.scene
		with tcnfile.open('model.xml', 'rU') as modelxml:
			def handle(parent, xml_parent):
				if parent is None and len(xml_parent):
					return False# this means we don't want to pursue the tree from here
				for element in xml_parent:
					local_parent = parent.append(element, factory)
					if not handle(local_parent, element):
						warning("Ignored handling of child {childtag} on element {parenttag}", childtag=element.tag, parenttag=xml_parent.tag)
				return True
			xml_root = ElementTree.parse(modelxml).getroot()
			techne = factory(xml_root)
			handle(techne, xml_root)
			objs = techne.apply()
			for obj in objs:
				scene.objects.link(obj)
			scene.update()
	op.report({'INFO'}, "Successfully imported techne-file {f}".format(f=file_p))
	return {'FINISHED'}

class ImportTechne(Operator, ImportHelper):
	bl_idname = "import_mesh.tcn"
	bl_label = "Import Techne Model"

	# ImportHelper mixin class uses this
	filename_ext = ".tcn"

	filter_glob = StringProperty(
			default="*.tcn",
			options={'HIDDEN'},
			)

	def execute(self, context):
		try:
			warning.active_op = self
			context.scene.render.engine = 'BLENDER_RENDER'
			context.scene.game_settings.material_mode = 'GLSL'
			context.user_preferences.system.use_mipmaps = False
			read_tcn(context, self.filepath, self)
		except FatalException as ex:
			self.report({'ERROR'}, "That should not have happened\n"+ex.value)
		else:
			return {'FINISHED'}
		return {'CANCELLED'}

def menu_func_import(self, context):
	self.layout.operator(ImportTechne.bl_idname, text="Techne Models (.tcn)")

def register():
	bpy.utils.register_class(ImportTechne)
	bpy.types.INFO_MT_file_import.append(menu_func_import)

def unregister():
	bpy.utils.unregister_class(ImportTechne)
	bpy.types.INFO_MT_file_import.remove(menu_func_import)

if __name__ == "__main__":
	register()
