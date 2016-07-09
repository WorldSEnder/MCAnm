from bpy_extras.io_utils import ImportHelper
import bmesh
import bpy
import os

from bpy.props import BoolProperty, CollectionProperty, EnumProperty, IntProperty,\
    IntVectorProperty, PointerProperty, StringProperty
from bpy.types import Operator

from .export import export_mesh, export_skl, export_action, MeshExportOptions,\
    SkeletonExportOptions, ActionExportOptions
from .imports import import_tabula, read_tcn
from .properties import SceneProps, Preferences
from .utils import Reporter, extract_safe, asset_to_dir


VERSIONS = [("V1", "Version 1", "Version 1 of MD model files. Rev1_010814"),
            ("V2", "Version 2", "Version 2 of MD model files. Rev1_300316")]
DEFAULT_VER = "V2"


class ObjectExporter(Operator):
    """Exporter to export an object to the .mcmd file format
    """
    bl_idname = "export_mesh.mcmd"
    bl_label = "Export MCMD"

    # Fileselector class uses this
    directory = StringProperty(
        name="Dir name",
        description="The resource folder to export to",
        subtype='DIR_PATH')
    model_path = Preferences.model_path
    mod_id = Preferences.mod_id
    proj_name = SceneProps.projectname

    uuid = IntVectorProperty(
        name="UUID",
        description="A unique ID for this file",
        options={'HIDDEN'},
        default=(0, 0, 0, 0),
        size=4)
    object = SceneProps.object

    version = EnumProperty(
        name="Version",
        description="Target version in Minecraft.",
        items=VERSIONS,
        default=DEFAULT_VER,
        options={'HIDDEN'})

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "proj_name")
        layout.prop(self, "version")
        layout.prop(self, "mod_id")
        layout.prop(self, "model_path")

    def execute(self, context):
        with Reporter() as reporter:
            obj = extract_safe(
                bpy.data.objects, self.object, "Object {item} not in bpy.data.objects!")
            # Note: we have to export an object, because vertex-groups are on
            # the object, not the mesh
            if obj.type != 'MESH':
                Reporter.error(
                    "Object {item} not a Mesh".format(item=self.object))
            mesh = obj.data
            modelpath = self.model_path.format(
                modid=self.mod_id,
                projectname=self.proj_name,
                modelname=obj.name)
            props = mesh.mcprops
            if props.armature:
                armature = extract_safe(
                    bpy.data.armatures, props.armature, "Armature {item} not in bpy.data.armatures")
                if armature not in [mod.object.data for mod in obj.modifiers if mod.type == 'ARMATURE' and mod.object is not None]:
                    Reporter.warning(
                        "Armature {arm} is not active on object {obj}", arm=props.armature, obj=self.object)
            else:
                armature = None

            opt = MeshExportOptions()
            opt.obj = obj
            opt.arm = armature
            opt.uv_layer = extract_safe(
                opt.obj.data.uv_layers, props.uv_layer, "Invalid UV-Layer {item}")

            opt.version = self.version
            opt.uuid = self.uuid
            opt.filepath = os.path.join(
                self.directory,
                asset_to_dir(modelpath))

            export_mesh(context, opt)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

    def invoke(self, context, event):
        if context.object is None or context.object.type != 'MESH':
            self.report(
                {'ERROR'}, "Active object must be a mesh not {}".format(context.object.type))
            return {'CANCELLED'}
        prefs = context.user_preferences.addons[__package__].preferences
        sceprops = context.scene.mcprops
        props = context.object.data.mcprops

        self.mod_id = prefs.mod_id
        if not self.mod_id:
            self.report({'ERROR'}, "mod_id is empty")
            return {'CANCELLED'}
        self.model_path = prefs.model_path
        if not self.model_path:
            self.report({'ERROR'}, "model_path is empty")
            return {'CANCELLED'}

        self.object = context.object.name
        self.armature = props.armature
        self.uv_layer = props.uv_layer
        if not self.uv_layer and context.object.data.uv_layers.active is not None:
            self.uv_layer = context.object.data.uv_layers.active.name

        self.artist = props.artist
        self.default_group_name = props.default_group.name
        self.default_img = props.default_group.image

        self.proj_name = sceprops.projectname
        self.uuid = sceprops.uuid

        self.directory = bpy.path.abspath(prefs.directory)
        context.window_manager.fileselect_add(self)
        return {'RUNNING_MODAL'}


class AnimationExporter(Operator):
    """Exports the active action of the selected armature
    """
    bl_idname = "export_anim.mcanm"
    bl_label = "Export MCANM"

    directory = StringProperty(
        name="Dir name",
        description="The resource folder to export to",
        subtype='DIR_PATH')
    animation_path = Preferences.animation_path
    mod_id = Preferences.mod_id
    proj_name = SceneProps.projectname

    skeleton = StringProperty(
        name="Armature",
        description="The armature this action is \"bound\" to",
        default=""
    )
    action = StringProperty(
        name="Action",
        description="The action to export"
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
        layout.prop(self, "proj_name")
        # layout.prop(self, "version")
        layout.prop(self, "mod_id")
        layout.prop(self, "animation_path")

    def execute(self, context):
        with Reporter() as reporter:
            skeleton = extract_safe(
                bpy.data.objects, self.skeleton, "Invalid skeleton: {item}, not in bpy.data.objects")
            if skeleton.type != 'ARMATURE':
                Reporter.error(
                    "Skeleton {obj} is not an armature", obj=skeleton.name)
            armature = skeleton.data
            action = extract_safe(
                bpy.data.actions, self.action, "Invalid action: {item}, not in bpy.data.actions")
            modelpath = self.animation_path.format(
                modid=self.mod_id,
                projectname=self.proj_name,
                animname=action.name)

            opts = ActionExportOptions()
            opts.action = action
            opts.armature = armature
            opts.filepath = os.path.join(
                self.directory,
                asset_to_dir(modelpath))
            opts.version = 0
            export_action(context, opts)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

    def invoke(self, context, event):
        if context.object.type != 'ARMATURE':
            self.report({'ERROR'}, "Active object {obj} is not an armature".format(
                obj=context.object.name))
            return {'CANCELLED'}
        self.skeleton = context.object.name
        try:
            self.action, props = AnimationExporter.guess_action(
                context.object)
        except ValueError as ex:
            self.report(
                {'ERROR'}, "Guessing action from active armature failed: {err}".format(err=ex))
            return {'CANCELLED'}

        prefs = context.user_preferences.addons[__package__].preferences
        sceprops = context.scene.mcprops

        self.mod_id = prefs.mod_id
        if not self.mod_id:
            self.report({'ERROR'}, "mod_id is empty")
            return {'CANCELLED'}
        self.animation_path = prefs.animation_path
        if not self.animation_path:
            self.report({'ERROR'}, "animation_path is empty")
            return {'CANCELLED'}
        self.directory = prefs.directory

        self.proj_name = sceprops.projectname

        context.window_manager.fileselect_add(self)
        return {'RUNNING_MODAL'}


class SkeletonExporter(Operator):
    """Exports the active action of the selected armature
    """
    bl_idname = "export_arm.mcanm"
    bl_label = "Export MCSKL"

    directory = StringProperty(
        name="Dir name",
        description="The resource folder to export to",
        subtype='DIR_PATH')
    skeleton_path = Preferences.skeleton_path
    mod_id = Preferences.mod_id
    proj_name = SceneProps.projectname

    uuid = IntVectorProperty(
        name="UUID",
        description="A unique ID for this file",
        options={'HIDDEN'},
        default=(0, 0, 0, 0),
        size=4)
    skeleton = SceneProps.skeleton

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
        layout.prop(self, "proj_name")
        # layout.prop(self, "version")
        layout.prop(self, "mod_id")
        layout.prop(self, "skeleton_path")

    def execute(self, context):
        with Reporter() as reporter:
            armature = extract_safe(
                bpy.data.objects, self.skeleton, "Invalid armature: {item}, not in bpy.data.objects")
            # the object, not the mesh
            if armature.type != 'ARMATURE':
                Reporter.error(
                    "Object {item} not an Armature".format(item=self.skeleton))
            modelpath = self.skeleton_path.format(
                modid=self.mod_id,
                projectname=self.proj_name,
                skeletonname=armature.name)

            opts = SkeletonExportOptions()
            opts.arm = armature
            opts.filepath = os.path.join(
                self.directory,
                asset_to_dir(modelpath))
            opts.uuid = self.uuid
            export_skl(context, opts)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

    def invoke(self, context, event):
        if context.object.type != 'ARMATURE':
            self.report({'ERROR'}, "Active object {obj} is not an armature".format(
                obj=context.object.name))
            return {'CANCELLED'}
        self.skeleton = context.object.name

        prefs = context.user_preferences.addons[__package__].preferences
        sceprops = context.scene.mcprops

        self.mod_id = prefs.mod_id
        if not self.mod_id:
            self.report({'ERROR'}, "mod_id is empty")
            return {'CANCELLED'}
        self.skeleton_path = prefs.skeleton_path
        if not self.skeleton_path:
            self.report({'ERROR'}, "animation_path is empty")
            return {'CANCELLED'}
        self.directory = prefs.directory
        self.proj_name = sceprops.projectname
        self.uuid = sceprops.uuid

        context.window_manager.fileselect_add(self)
        return {'RUNNING_MODAL'}


class SceneExporter(Operator):
    bl_idname = "export_scene.mcanm"
    bl_label = "Export Scene to Minecraft"

    scene = StringProperty(
        name="Scene",
        description="The scene to export fully")
    directory = Preferences.directory
    model_path = Preferences.model_path
    animation_path = Preferences.animation_path
    skeleton_path = Preferences.skeleton_path
    mod_id = Preferences.mod_id

    def draw(self, context):
        layout = self.layout
        layout.prop_search(self, 'scene', bpy.data, 'scenes')
        layout.prop(self, 'mod_id')
        layout.prop(self, 'model_path')
        layout.prop(self, 'animation_path')
        layout.prop(self, 'skeleton_path')

    def execute(self, context):
        with Reporter() as reporter:
            scene = extract_safe(
                bpy.data.scenes, self.scene, "Scene {item} not found")
            sceprops = scene.mcprops
            if sceprops.object in bpy.data.objects:
                eval('bpy.ops.' + ObjectExporter.bl_idname)(
                    directory=self.directory,
                    model_path=self.model_path,
                    mod_id=self.mod_id,
                    proj_name=sceprops.projectname,
                    uuid=sceprops.uuid,
                    object=sceprops.object)
            if sceprops.skeleton in bpy.data.objects:
                eval('bpy.ops.' + SkeletonExporter.bl_idname)(
                    directory=self.directory,
                    skeleton_path=self.skeleton_path,
                    mod_id=self.mod_id,
                    proj_name=sceprops.projectname,
                    uuid=sceprops.uuid,
                    skeleton=sceprops.skeleton)
            ActionExporter = eval('bpy.ops.' + AnimationExporter.bl_idname)
            for act in bpy.data.actions:
                if act.mcprops.scene not in {'', self.scene}:
                    continue
                ActionExporter(
                    directory=self.directory,
                    mod_id=self.mod_id,
                    proj_name=sceprops.projectname,
                    skeleton=sceprops.skeleton,
                    action=act.name)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

    def invoke(self, context, event):
        prefs = context.user_preferences.addons[__package__].preferences
        scene = context.scene
        self.scene = scene.name

        self.directory = prefs.directory
        self.model_path = prefs.model_path
        self.animation_path = prefs.animation_path
        self.skeleton_path = prefs.skeleton_path
        self.mod_id = prefs.mod_id

        context.window_manager.fileselect_add(self)
        return {'RUNNING_MODAL'}


class TabulaImport(Operator, ImportHelper):
    bl_idname = "tabula.load"
    bl_label = "Import Tabula model (.tbl)"

    filename_ext = ".tbl"
    filter_glob = StringProperty(default="*.tbl", options={'HIDDEN'})
    filepath = StringProperty(name="File Path", default="")

    only_poses = EnumProperty(items=[("poses", "Animations", "Import the contained animations"),
                                     ("model", "Model", "Import the model from the file")],
                              name="Import strategy",
                              default={"poses", "model"},
                              options={'ENUM_FLAG'})
    scene = StringProperty(
        name="Scene to import into", default="", options={'HIDDEN'})

    def draw(self, context):
        layout = self.layout
        layout.prop(self, 'only_poses')
        layout.prop_search(self, 'scene', bpy.data, 'scenes', icon="SCENE")

    def execute(self, context):
        with Reporter() as reporter:
            sce = extract_safe(
                bpy.data.scenes, self.scene, 'Scene {item} not found')
            import_tabula(self.filepath, sce, self.only_poses)
            reporter.info(
                "Successfully imported the Tabula model from {path}", path=self.filepath)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}

    def invoke(self, context, event):
        self.scene = context.scene.name
        return super().invoke(context, event)


class TechneImport(Operator, ImportHelper):
    bl_idname = "import_mesh.tcn"
    bl_label = "Import Techne Model"

    # ImportHelper mixin class uses this
    filename_ext = ".tcn"

    filter_glob = StringProperty(
        default="*.tcn",
        options={'HIDDEN'},
    )

    def execute(self, context):
        with Reporter() as reporter:
            context.scene.render.engine = 'BLENDER_RENDER'
            context.scene.game_settings.material_mode = 'GLSL'
            context.user_preferences.system.use_mipmaps = False
            read_tcn(context, self.filepath, self)
            reporter.info(
                "Successfully imported the techne model from {path}", path=self.filepath)
        reporter.print_report(self)
        return {'FINISHED'} if reporter.was_success() else {'CANCELLED'}
    # TODO: priority2 finish


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
        props.active_render_group = len(groups)
        g = groups.add()
        g.name = "Default"
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
