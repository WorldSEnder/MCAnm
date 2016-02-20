import bmesh
import bpy
import re
import os

from .utils import Reporter, write_string, write_packed, asset_to_dir, openw_save, extract_safe
from contextlib import ExitStack
from collections import defaultdict

class Bone(object):
	def __init__(self, bone):
		"""
		bone Bone: can't be None, the bone this is initialized from
		"""
		if bone is None:
			Reporter.fatal("(Bone) bone is None")
		self.name = bone.name
		mat = bone.matrix_local
		if bone.parent is not None:
			# this is a little quick fix we have to do, don't ask me...
			mat = bone.parent.matrix_local.inverted() * mat
		self.quat = mat.to_quaternion()
		self.translate = mat.translation
	def to_bytes(self, file_h):
		q = self.quat
		t = self.translate
		write_string(self.name, file_h)
		write_packed(">7f", file_h, q.x, q.y, q.z, q.w,
								  t.x, t.y, t.z)

class Point(object):
	def __init__(self, loop, uv_layer, deform_layer, arm_vgroup_idxs):
		"""
		loop Loop: Can't be none, the loop this points represents
		uv_layer LLayerItem: Can't be None, the uv_layer we export
		deform_layer VLayerItem: Can be None, indicates that no bindings
								 should be exported
		arm_vgroup_idxs list: Can't be None, value at index i represents the index of the
							  arm.bones[i]'s vertex group in object
		"""
		if loop is None:
			Reporter.fatal("(Point) Loop is None")
		vtx = loop.vert
		self.coords = vtx.co
		self.normal = vtx.normal
		self.uv = loop[uv_layer].uv
		self.bindings = []
		# calculate bindings
		if deform_layer is None:
			return
		def insert_into_binds(bind):
			"""
			bind tuple: Can't be None, (idx, value) of binding
			"""
			if bind is None:
				Reporter.fatal("(Point) Inserted tupple is None")
			self.bindings.append(bind)
		# iterate over all bones (as indices)
		for idx, vgroup_idx in enumerate(arm_vgroup_idxs):
			if vgroup_idx == -1:
				# bone has no vertex group
				continue
			try:
				value = vtx[deform_layer][vgroup_idx]
			except KeyError:
				# no binding to this bone
				pass
			else:
				if value > 0:
					insert_into_binds((idx, value))
	def to_bytes(self, file_h):
		binds = sorted(self.bindings, key=lambda x: x[1])[-4:]
		co = self.coords
		norm = self.normal
		uv = self.uv
		write_packed(
				">8f" + "Bf" * len(binds),
				file_h,
				co.x, co.y, co.z,
				norm.x, norm.y, norm.z,
				uv.x, 1 - uv.y,
				*[v for bind in binds for v in bind])
		if len(binds) < 4:
			file_h.write(b'\xFF')
	def __eq__(self, other):
		epsilon = 0.001
		return  (self.bindings == other.bindings and
				(self.uv - other.uv).length < epsilon and
				(self.coords - other.coords).length < epsilon and
				(self.normal - other.normal).length < epsilon)
	def __str__(self):
		return "Point(co={co}, normal={norm}, uv={uv}, bindings={binds})".format(
				co=self.coords,
				norm=self.normal,
				uv=self.uv,
				binds=self.bindings)

class Part(object):
	def __init__(self, group, options):
		"""
		group Group: can be None
			in case of None, it is the default group
		"""
		img = options.def_image
		if group is None:
			self.name = options.def_group_name
		elif group.image in bpy.data.images:
			self.name = group.name
			img = group.image
		else:
			self.name = group.name
			Reporter.warning("Group {name} has an invalid image ({img}) assigned. Defaulted to {default}",
					name=self.name, img=group.image, default=options.def_image)
			img = options.def_image.name
		self.image = bpy.data.images[img]
		self.img_location = options.texpath.format(
						modid=options.mod_id,
						modelname=options.modelname,
						texname=bpy.path.ensure_ext(img, '.png')[:-4])
		# maps vertices to (point, list-index) pair-list
		self.point_map = defaultdict(lambda: [])
		# a full list of all points
		self.points = []
		self.indices = []
	def append_face(self, face, uv_layer, deform_layer, arm_vgroup_idxs):
		"""
		uv_layer LLayerItem: Can't be None, the uv_layer we export
		deform_layer VLayerItem: Can be None, indicates that no bindings
								 should be exported
		arm_vgroup_idxs list: Can't be None, value at index i represents the index of the
							  arm.bones[i]'s vertex group in object
		"""
		def key_from_loop(l):
			return l.vert.index
		for loop in face.loops:
			key = key_from_loop(loop)
			# append points or reuse existant one's
			new_point = Point(loop, uv_layer, deform_layer, arm_vgroup_idxs)
			index = -1
			known_points = self.point_map[key]
			for point, idx in known_points:
				if point == new_point:
					index = idx
					break
			else:
				index = len(self.points)
				self.points.append(new_point)
				known_points.append((new_point, index))
			assert index >= 0
			self.indices.append(index)
	def to_bytes(self, file_h):
		points = self.points
		p_len = len(self.points)
		idxs = self.indices
		idxs_len = len(idxs)
		if idxs_len % 3:
			Reporter.fatal("(Part) number of indices not divisible by 3")
		tris = idxs_len//3
		if tris >= 2**16:
			Reporter.error("(Part) too many tris in part {name}", name=self.name)
		write_packed(">2H", file_h, p_len, tris)
		write_string(self.name, file_h)
		write_string(self.img_location, file_h)
		for point in points:
			point.to_bytes(file_h)
		write_packed(">{idxs}H".format(idxs=idxs_len), file_h, *idxs)

class Animation(object):
	def __init__(self, fcurve, start):
		self.extrapolation_mode = 'CONSTANT'
		self.offset = start
		if fcurve is None:
			self.points = []
		else:
			self.points = [keyf for keyf in fcurve.keyframe_points]
			self.extrapolation_mode = fcurve.extrapolation
	def to_file(self, file_h):
		interpolations = {'CONSTANT': 8, 'LINEAR': 9, 'BEZIER': 10}
		extrapolations = {'CONSTANT': 16, 'LINEAR': 17}
		
		points = self.points
		def write_point(co):
			write_packed(">2f", file_h, co[0] - self.offset, co[1])
		anim_len = len(points)
		if anim_len > 2**16 - 1:
			Reporter.error("Too many keyframes in fcurve to export")
		# animation length
		write_packed(">H", file_h, anim_len)
		if anim_len == 0:
			return
		write_point(points[0].co)
		# TUNE IN
		write_packed(">B", file_h, 0) # = LINEAR
		# POINTS
		for left, right in zip(points[:], points[1:]):
			write_point(right.co)
			raw_mode = right.interpolation
			interpolation_code = extract_safe(interpolations, raw_mode, "Unknown interpolation mode {item}")
			write_packed(">B", file_h, interpolation_code)
			if raw_mode == 'BEZIER':
				write_point(left.handle_right)
				write_point(right.handle_left)
		# TUNE OUT
		extrapolation_code = extract_safe(extrapolations, self.extrapolation_mode, "Unknown extrapolation mode {item}")
		write_packed(">B", file_h, extrapolation_code)
		if self.extrapolation_mode == 'LINEAR':
			write_point(points[-1].handle_right)

class BoneAction(object):
	def __init__(self, name, curves, offset):
		loc_curves, rot_curves, scale_curves = curves
		self.name = name
		self.loc_x = Animation(loc_curves[0], offset)
		self.loc_y = Animation(loc_curves[1], offset)
		self.loc_z = Animation(loc_curves[2], offset)

		self.rot_w = Animation(rot_curves[0], offset)
		self.rot_x = Animation(rot_curves[1], offset)
		self.rot_y = Animation(rot_curves[2], offset)
		self.rot_z = Animation(rot_curves[3], offset)

		self.scale_x = Animation(scale_curves[0], offset)
		self.scale_y = Animation(scale_curves[1], offset)
		self.scale_z = Animation(scale_curves[2], offset)
	
	def to_file(self, file_h):
		write_string(self.name, file_h)
		self.loc_x.to_file(file_h)
		self.loc_y.to_file(file_h)
		self.loc_z.to_file(file_h)

		self.rot_x.to_file(file_h)
		self.rot_y.to_file(file_h)
		self.rot_z.to_file(file_h)
		self.rot_w.to_file(file_h)

		self.scale_x.to_file(file_h)
		self.scale_y.to_file(file_h)
		self.scale_z.to_file(file_h)

class MeshExportOptions(object):
	mod_id = None
	dirpath = None
	modelpath = None
	texpath = None

	obj = None
	arm = None
	uv_layer = None
	version = None

	artist = None
	modelname = None
	def_group_name = None
	def_image = None

	export_tex = None

def export_mesh_v1(context, options, file_h):
	# extract used values
	obj = options.obj
	mod_id = options.mod_id
	tex_path = options.texpath
	model_name = options.modelname
	# the object we export
	with ExitStack() as stack:
		if context.mode == 'EDIT_MESH':
			bmc = bmesh.from_edit_mesh(obj.data)
			stack.callback(bmesh.update_edit_mesh, obj.data)
			bm = bmc.copy()
			stack.callback(bm.free)
		else:
			bm = bmesh.new()
			stack.callback(bm.free)
			bm.from_mesh(obj.data)
		bmesh.ops.triangulate(bm, faces=bm.faces)
		# the armature to that object
		arm = options.arm
		# for each material we make one part
		part_dict = {}
		# checked
		uv_layer = bm.loops.layers.uv[options.uv_layer.name]
		# could be None
		deform_layer = bm.verts.layers.deform.active
		# never none
		arm_vgroup_idxs = ([] if arm is None else
						   [obj.vertex_groups.find(bone.name) for bone in arm.bones])
		g_layer = None
		if 'MCRenderGroupIndex' in bm.faces.layers.int:
			g_layer = bm.faces.layers.int['MCRenderGroupIndex']
		groups = obj.data.mcprops.render_groups
		for face in bm.faces:
			g_idx = face[g_layer] - 1 if g_layer is not None else -1
			group = groups[g_idx] if g_idx >= 0 and g_idx < len(groups) else None
			if group not in part_dict:
				part_dict.update({group: Part(group, options)})
			part_dict[group].append_face(face, uv_layer, deform_layer, arm_vgroup_idxs)
		if len(part_dict) > 255:
			Reporter.error("Too many parts")
		bones = ([] if arm is None else
				 [Bone(bone) for bone in arm.bones])
		if len(bones) > 255:
			Reporter.error("Too many bones")
		bone_parents = ([] if arm is None else
						[arm.bones.find(b.parent.name) & 0xFF if b.parent is not None else 255 for b in arm.bones])

		# write the stuff
		write_packed(">I", file_h, 1)
		write_packed(">2B", file_h, len(part_dict), len(bones))
		for key in part_dict.keys():
			part = part_dict[key]
			part.to_bytes(file_h)
		for bone in bones:
			bone.to_bytes(file_h)
		write_packed(">{nums}B".format(nums=len(bones)), file_h, *bone_parents)

		if options.export_tex:
			settings = context.scene.render.image_settings
			settings.file_format = 'PNG'
			settings.color_mode = 'RGBA'
			for img, path in set([(p.image, p.img_location) for p in part_dict.values()]):
				ext_path =\
						os.path.join(
								options.dirpath,
								asset_to_dir(path))
				img.save_render(bpy.path.abspath(ext_path), scene=context.scene)
	
	summary = 'Exported with ({numparts} parts, {numbones} bones)'.format(
				numparts=len(part_dict),
				numbones=len(bones)
			)
	Reporter.info(summary)

# exporters also have to write their version number
known_exporters = {"V1": export_mesh_v1}

def export_mesh(context, options):
	scene = context.scene
	# Write file header
	bitmask = 0xFFFFFFFF
	uuid_vec = scene.mcprops.uuid
	# Write to file
	modelpath = options.modelpath.format(
			modid = options.mod_id,
			modelname = options.modelname)
	filepath = os.path.join(
			options.dirpath,
			asset_to_dir(modelpath))
	with openw_save(filepath, 'wb') as file_h:
		file_h.write(b'MHFC MDL')
		write_packed(">4I", file_h,
				uuid_vec[0] & bitmask,
				uuid_vec[1] & bitmask,
				uuid_vec[2] & bitmask,
				uuid_vec[3] & bitmask)
		write_string(options.artist, file_h)
		try:
			known_exporters[options.version](context, options, file_h)
		except (KeyError, NotImplementedError) as ex:
			Reporter.fatal("Version {v} is not implemented yet", v=options.version)
def export_action(filepath, action, offset, artist, armature):
	bone_data_re = re.compile("pose\.bones\[\"(.*)\"\]\.(rotation_quaternion|location|scale)")
	bone_dict = defaultdict(lambda: ([None]*3, [None]*4, [None]*3))
	armBones = armature.bones
	curves = 0
	for curve in action.fcurves:
		match = bone_data_re.match(curve.data_path)
		if match is None:
			continue
		bone, key = match.groups()
		if bone not in armBones:
			continue
		if key == 'location' and not armBones[bone].use_connect:
			bone_dict[bone][0][curve.array_index] = curve
			curves += 1
		elif key == 'rotation_quaternion':
			bone_dict[bone][1][curve.array_index] = curve
			curves += 1
		elif key == 'scale':
			bone_dict[bone][2][curve.array_index] = curve
			curves += 1
	bone_count = len(bone_dict)
	if bone_count > 255:
		Reporter.error("Too many bones to export")
	with open(filepath, 'wb') as file_h:
		file_h.write(b'MHFC ANM')
		write_string(artist, file_h)
		write_packed(">B", file_h, bone_count)
		for bone in bone_dict:
			BoneAction(bone, bone_dict[bone], offset).to_file(file_h)
	summary = 'Exported {bones} bones with {curves} animated values'.format(
			bones=bone_count,
			curves=curves
		)
	Reporter.info(summary)
