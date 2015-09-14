import bpy
import bmesh

from .export import export_mesh, export_action, MeshExportOptions
from .utils import Reporter, extract_safe
from bpy.props import BoolProperty, CollectionProperty, EnumProperty, IntProperty,\
		IntVectorProperty, PointerProperty, StringProperty
from bpy_extras.io_utils import ExportHelper
from bpy.types import Operator
from contextlib import ExitStack

VERSIONS = [("V1", "Version 1", "Version 1 of MD model files. Rev1_010814")]
DEFAULT_VER = "V1"

class ObjectExporter(Operator):
	"""Exporter to export an object to the .mcmd file format
	"""
	bl_idname = "export_mesh.mcmd"
	bl_label = "Export MCMD"

	# Fileselector class uses this
	mod_id = StringProperty(
			name="Mod ID",
			description="Your Mod ID",
			default="minecraft")
	filepath = StringProperty(
			name="Dir name",
			description="The resource folder to export to",
			subtype='DIR_PATH')
	model_path = StringProperty(
			name="Path to the model",
			description="A formatstring to the path of you model. You may use {modid} and {modelname}.",
			default="{modid}:models/{modelname}/{modelname}.mcmd",
			options={'HIDDEN'})
	tex_path = StringProperty(
			name="Path to the textures",
			description="A formatstring to the textures. You may use {modid}, {modelname} and {texname}.",
			default="{modid}:textures/models/{modelname}/{texname}.png",
			options={'HIDDEN'})

	object = StringProperty(
			name="Object",
			description="The object to export",
			options=set())
	armature = StringProperty(
			name="Armature",
			description="The armature that defines animations.")
	uv_layer = StringProperty(
			name="UV Layer",
			description="The uv layer for texture mappings")

	version = EnumProperty(
			name="Version",
			description="Target version in Minecraft.",
			items=VERSIONS,
			default=DEFAULT_VER,
			options={'HIDDEN'})
	artist = StringProperty(
			name="Artist name",
			description="Your name")
	model_name = StringProperty(
			name="Model Name",
			description="The name of your model. ")
	default_group_name = StringProperty(
			name="Default Group",
			description="The group all faces are in if not in a valid group",
			default="Default")
	default_img = StringProperty(
			name="Default Tex",
			description="The texture a face gets if it doesn't specify a specific one.")

	export_tex = BoolProperty(
			name="Export Textures",
			description="Whether to export textures or not")

	def draw(self, context):
		layout = self.layout
		layout.prop_search(self, "armature", bpy.data, "armatures", icon="ARMATURE_DATA")
		if self.object:
			layout.prop_search(self, "uv_layer", bpy.data.objects[self.object].data, "uv_layers", icon="GROUP_UVS")

		layout.prop(self, "version")
		layout.prop(self, "artist")
		layout.prop(self, "model_name")
		box = layout.box()
		box.prop(self, "default_group_name")
		box.prop_search(self, "default_img", bpy.data, "images")

		layout.prop(self, "export_tex")
		layout.prop(self, "model_path")
		layout.prop(self, "tex_path")

	def execute(self, context):
		with Reporter() as reporter:
			opt = MeshExportOptions()

			opt.mod_id = self.mod_id
			opt.dirpath = self.filepath
			opt.modelpath = self.model_path
			opt.texpath = self.tex_path

			opt.obj = extract_safe(bpy.data.objects, self.object, "Object {item} not in bpy.data.objects!")
			# Note: we have to export an object, because vertex-groups are on the object, not the mesh
			if opt.obj.type != 'MESH':
				Reporter.error("Active Object not a Mesh")
			if self.armature:
				opt.arm = extract_safe(bpy.data.armatures, self.armature, "Armature {item} not in bpy.data.armatures")
				if opt.arm not in [mod.object.data for mod in opt.obj.modifiers if mod.type == 'ARMATURE' and mod.object is not None]:
					Reporter.warning("Armature {arm} is not active on object {obj}", arm=self.armature, obj=self.object)
			else:
				opt.arm = None
			opt.uv_layer = extract_safe(opt.obj.data.uv_layers, self.uv_layer, "Invalid UV-Layer {item}")

			opt.version = self.version
			opt.artist = self.artist
			opt.modelname = self.model_name
			opt.def_group_name = self.default_group_name
			if opt.def_group_name in opt.obj.data.mcprops.render_groups:
				Reporter.error("Default groupname can't duplicate group name")
			opt.def_image = extract_safe(bpy.data.images, self.default_img, "Default image {item} not in bpy.data.images").name
			opt.export_tex = self.export_tex

			context.scene.mcprops.export_tex = False
			export_mesh(context, opt)
		reporter.print_report(self)
		return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

	def invoke(self, context, event):
		if context.object is None or context.object.type != 'MESH':
			self.report({'ERROR'}, "Active object must be a mesh")
			return {'CANCELLED'}
		prefs = context.user_preferences.addons[__package__].preferences
		props = context.object.data.mcprops
		sceprops = context.scene.mcprops

		self.mod_id = prefs.mod_id
		if not self.mod_id:
			self.mod_id = 'minecraft'
		self.model_path = prefs.model_path
		if not self.model_path:
			self.model_path = '{modid}:models/{modelname}/{modelname}.mcmd'
		self.tex_path = prefs.tex_path
		if not self.tex_path:
			self.tex_path = '{modid}:textures/models/{modelname}/{texname}.png'

		self.object = context.object.name
		self.armature = props.armature
		self.uv_layer = props.uv_layer
		if not self.uv_layer and context.object.data.uv_layers.active is not None:
			self.uv_layer = context.object.data.uv_layers.active.name

		self.version = props.version
		self.artist = props.artist
		self.model_name = props.name
		self.model_name = bpy.path.ensure_ext(self.model_name, '.mcmd')[:-5]
		self.model_name = self.model_name.replace(' ', '_').replace('/', '_').replace('\\', '_')
		self.default_group_name = props.default_group_name
		self.default_img = props.default_img

		self.export_tex = sceprops.export_tex

		self.filepath = bpy.path.abspath(prefs.directory)
		context.window_manager.fileselect_add(self)
		return {'RUNNING_MODAL'}

class ArmAnimationExporter(Operator):
	"""Exports the active action of the selected armature
	"""
	bl_idname = "export_anim.mcanm"
	bl_label = "Export MCANM"

	filename_ext = ".mcanm"

	filepath = StringProperty(
			subtype="FILE_PATH"
			)
	
	filter_glob = StringProperty(
			default="*.mcanm",
			options={'HIDDEN'},
			)
	
	arm_action = StringProperty(
			name="Action",
			description="The action to export",
			)
	
	artist = StringProperty(
			name="Artist",
			description="The artist of this action",
			default=""
			)
	
	armature = StringProperty(
			name="Armature",
			description="The armature this action is \"bound\" to",
			default=""
			)
	
	offset = IntProperty(
			name="Animation begin",
			description="The frame the animation should begin at",
			default=0
			)

	@staticmethod
	def guess_action(obj):
		anim_data = obj.animation_data
		if anim_data is None:
			raise ValueError("Active object has no animation data")
		action = anim_data.action
		if action is None:
			raise ValueError("Active object has no action bound")
		return action.name, action.mcprops
	
	def draw(self, context):
		layout = self.layout
		layout.prop(self, "artist")
		layout.prop_search(self, "armature", bpy.data, "armatures", icon="ARMATURE_DATA")
		layout.prop_search(self, "arm_action", bpy.data, "actions", icon="ANIM_DATA")
		layout.prop(self, "offset")

	def execute(self, context):
		with Reporter() as reporter:
			armature = extract_safe(bpy.data.armatures, self.armature, "Invalid armature: {item}, not in bpy.data.armatures")
			action = extract_safe(bpy.data.actions, self.arm_action, "Invalid action: {item}, not in bpy.data.actions")
			export_action(self.filepath, action, self.offset, self.artist, armature)
		reporter.print_report(self)
		return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

	def invoke(self, context, event):
		if context.object.type != 'ARMATURE':
			self.report({'ERROR'}, "Active object {obj} is not an armature", obj=context.object.name)
			return {'CANCELLED'}
		self.armature = context.object.data.name
		try:
			self.arm_action, props = ArmAnimationExporter.guess_action(context.object)
		except ValueError as ex:
			self.report({'ERROR'}, "Guessing action from active armature failed: {err}", err=ex.value)
			return {'CANCELLED'}
		self.offset = props.offset
		context.window_manager.fileselect_add(self)
		return {'RUNNING_MODAL'}

class ArmatureUpdater(Operator):
	bl_idname = "object.mc_update_arms"
	bl_label = "Update possible armatures"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'MESH'

	def execute(self, context):
		obj = context.object
		armlist = obj.data.mcprops.poss_arms
		armlist.clear()
		for mod in obj.modifiers:
			if mod.type != 'ARMATURE' or mod.object is None:
				continue
			item = armlist.add()
			item.name = mod.object.name
		return {'FINISHED'}

class AddRenderGroup(Operator):
	"""Adds a group of points.
	"""
	bl_idname = "object.mc_group_add"
	bl_label = "Add render group"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'MESH'

	def execute(self, context):
		data = context.object.data
		props = data.mcprops
		groups = props.render_groups
		g = groups.add()
		g.name = "Default"
		props.active_render_group = len(groups)
		return {'FINISHED'}

class RemoveRenderGroup(Operator):
	"""Removes the active render group.
	"""
	bl_idname = "object.mc_group_remove"
	bl_label = "Add render group"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'MESH' and context.object.data.mcprops.active_render_group >= 0

	def execute(self, context):
		data = context.object.data
		props = data.mcprops
		groups = props.render_groups
		group_idx = props.active_render_group
		groups.remove(group_idx)
		props.active_render_group = max(
			0 if len(groups) > 0 else -1,
			group_idx - 1)
		if context.mode == 'EDIT_MESH':
			bm = bmesh.from_edit_mesh(data)
			if 'MCRenderGroupIndex' not in bm.faces.layers.int:
				return {'FINISHED'}
			g_layer = bm.faces.layers.int['MCRenderGroupIndex']
			for face in bm.faces:
				face_idx = face[g_layer]
				if face_idx == group_idx + 1:
					face_idx = 0
				elif face_idx > group_idx + 1:
					face_idx -= 1
				face[g_layer] = face_idx
			bmesh.update_edit_mesh(data)
		else:
			if 'MCRenderGroupIndex' not in data.polygon_layers_int:
				return {'FINISHED'}
			for face in data.polygon_layers_int['MCRenderGroupIndex'].data:
				face_idx = face.value
				if face_idx == group_idx + 1:
					face_idx = 0
				elif face_idx > group_idx + 1:
					face_idx -= 1
				face.value = face_idx
		return {'FINISHED'}

class AddFacesToGroup(Operator):
	"""Adds all selected faces to the current render group.
	They can't be removed though: Ultimately no face should be without group
	"""
	bl_idname = "object.mc_group_add_faces"
	bl_label = "Assign"

	@classmethod
	def poll(cls, context):
		if context.mode != 'EDIT_MESH' or context.object is None or context.object.type != 'MESH':
			return False
		props = context.object.data.mcprops
		curr = props.active_render_group
		return curr >= 0 and curr < len(props.render_groups)

	def execute(self, context):
		data = context.object.data
		active_group = data.mcprops.active_render_group
		bm = bmesh.from_edit_mesh(data)
		if 'MCRenderGroupIndex' not in bm.faces.layers.int:
			bm.faces.layers.int.new('MCRenderGroupIndex')
		g_layer = bm.faces.layers.int['MCRenderGroupIndex']
		for face in bm.faces:
			if not face.select:
				continue
			# active_group + 1, because 0 means uninitialized
			face[g_layer] = active_group + 1
		bmesh.update_edit_mesh(data)
		return {'FINISHED'}

class SelectGroup(Operator):
	"""Selects all faces which belong to the current group
	"""
	bl_idname = "object.mc_group_select_faces"
	bl_label = "Select"

	@classmethod
	def poll(cls, context):
		if context.mode != 'EDIT_MESH' or context.object is None or context.object.type != 'MESH':
			return False
		props = context.object.data.mcprops
		curr = props.active_render_group
		return curr >= 0 and curr < len(props.render_groups) and 'MCRenderGroupIndex' in context.object.data.polygon_layers_int

	def execute(self, context):
		data = context.object.data
		active_group = data.mcprops.active_render_group
		bm = bmesh.from_edit_mesh(data)
		g_layer = bm.faces.layers.int['MCRenderGroupIndex']
		for face in bm.faces:
			face.select = (face[g_layer] == active_group + 1)
		bmesh.update_edit_mesh(data)
		return {'FINISHED'}

class UpdateGroupsVisual(Operator):
	"""Updates the Blender-internal texture assignement
	"""
	bl_idname = "object.mc_group_update_tex"
	bl_label = "Update Textures"

	mode = EnumProperty(
			items=[	("ALL", "All Faces", "Updates all faces on the mesh"),
					("SELECTED", "Selected Faces", "Updates only selected faces")],
			name="Mode",
			default="ALL")

	@classmethod
	def poll(cls, context):
		if context.mode != "EDIT_MESH":
			return False
		obj = context.object
		if obj is None or obj.type != "MESH":
			return False
		data = obj.data
		return obj.data.uv_layers.active is not None and 'MCRenderGroupIndex' in data.polygon_layers_int

	def execute(self, context):
		data = context.object.data
		bm = bmesh.from_edit_mesh(data)
		groups = data.mcprops.render_groups
		g_lyr = bm.faces.layers.int['MCRenderGroupIndex']
		tex_lyr = bm.faces.layers.tex.active
		for face in bm.faces:
			if self.mode == "SELECTED" and not face.select:
				continue
			idx = face[g_lyr] - 1
			if idx < 0:
				continue
			if idx < len(groups) and groups[idx].image in bpy.data.images:
				face[tex_lyr].image = bpy.data.images[groups[idx].image]
			else:
				face[g_lyr] = 0
		bmesh.update_edit_mesh(data)
		context.scene.render.engine = "BLENDER_RENDER"
		return {'FINISHED'}
