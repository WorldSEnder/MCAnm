import bpy
import random
import re

from bpy.app.handlers import persistent
from bpy.props import BoolProperty, CollectionProperty, EnumProperty, IntProperty,\
    IntVectorProperty, PointerProperty, StringProperty
from bpy.types import AddonPreferences, PropertyGroup, Scene, Mesh, Action


# helper methods for properties
def get_ifnot_gen(prop, generator, init_name=None):
    """
    This generates a new value if necessary before returning it.
    prop: the properties own name
    generator: a function that takes two parameters:
                    the object it is called on (e.g. the scene)
                    the identifier of the value
                    returns - the new value
    init_name: the name of a value that tells if this value
                    initialized. If init_name is None: returns a function
                    that checks against the actual value
    """

    def get_with_init(self):
        if not getattr(self, init_name):
            self[prop] = generator(self, prop)
            setattr(self, init_name, True)
        return self[prop]

    def get(self):
        if not self[prop]:
            self[prop] = generator(self, prop)
        return self[prop]
    return get_with_init if init_name is not None else get


def gen_rand(obj, name):
    next = lambda: random.randint(-2**31, 2**31 - 1)
    return (next(), next(), next(), next())


def update_default_ifnot(prop, default):
    def update(self, context):
        if not getattr(self, prop):
            setattr(self, prop, default)
    if not default:
        raise ValueError(
            "Default must test True for 'if default' to ensure no recursion occurs.")
    return update


class Preferences(AddonPreferences):
    bl_idname = __package__

    mod_id = StringProperty(
        name="Mod ID",
        description="Your Mod ID",
        default="minecraft",
        update=update_default_ifnot('mod_id', "minecraft"),
        options={'HIDDEN'})
    directory = StringProperty(
        name="Resource Folder",
        description="The resource folder of your mod, most likely 'C:\path\to\mod\src\main\resources\'",
        subtype='DIR_PATH',
        options={'HIDDEN'})
    model_path = StringProperty(
        name="Model Path",
        description="Advanced: A formatstring to the path of your models." +
        " You may use {modid}, {projectname} and {modelname}.",
        default="{modid}:models/{projectname}/{modelname}.mcmd",
        update=update_default_ifnot(
            'model_path', "{modid}:models/{projectname}/{modelname}.mcmd"),
        options={'HIDDEN'})
    skeleton_path = StringProperty(
        name="Model Path",
        description="Advanced: A formatstring to the path of your skeletons." +
        " You may use {modid}, {projectname} and {skeletonname}.",
        default="{modid}:models/{projectname}/{skeletonname}.mcskl",
        update=update_default_ifnot(
            'skeleton_path', "{modid}:models/{projectname}/{skeletonname}.mcskl"),
        options={'HIDDEN'})
    animation_path = StringProperty(
        name="Animation Path",
        description="Advanced: A formatstring to the path of you model." +
        " You may use {modid}, {projectname} and {animname}.",
        default="{modid}:models/{projectname}/{animname}.mcanm",
        update=update_default_ifnot(
            'animation_path', "{modid}:models/{projectname}/{animname}.mcanm"),
        options={'HIDDEN'})
    tex_path = StringProperty(
        name="Texpath",
        description="Advanced: A formatstring to the textures." +
        " You may use {modid}, {modelname} and {texname}.",
        default="{modid}:textures/models/{modelname}/{texname}.png",
        update=update_default_ifnot(
            'tex_path', "{modid}:textures/models/{modelname}/{texname}.png"),
        options={'HIDDEN'})

    def draw(self, context):
        layout = self.layout
        layout.prop(self, 'mod_id')
        layout.prop(self, 'directory')
        layout.prop(self, 'model_path')
        layout.prop(self, 'animation_path')
        layout.prop(self, 'tex_path')


class RenderGroups(PropertyGroup):
    image = StringProperty(
        name="Image",
        description="The image-texture of this group")


class SceneProps(PropertyGroup):
    isuuidset = BoolProperty(
        name="isUUIDset",
        description="Set to true if scene data is initialized. DON'T TOUCH THIS UNLESS YOU ARE SURE WHAT YOU DO.",
        default=False,
        options={'HIDDEN'})
    uuid = IntVectorProperty(
        name="UUID",
        description="An unique ID for this file. Read-only",
        options={'HIDDEN'},
        default=(0, 0, 0, 0),
        size=4,
        get=get_ifnot_gen('uuid', gen_rand, 'isuuidset'))
    export_tex = BoolProperty(
        name="Export Textures",
        description="Whether to export textures or not",
        default=False,
        options={'HIDDEN'})
    enable_advanced = BoolProperty(
        name="Adv. settings",
        description="Enables advanced settings",
        default=False,
        options={'HIDDEN', 'SKIP_SAVE'})
    projectname = StringProperty(
        name="Model Name",
        description="The name of your model. ",
        subtype='FILE_NAME',
        options=set())

    @classmethod
    def register(cls):
        Scene.mcprops = PointerProperty(type=SceneProps)

    @classmethod
    def unregister(cls):
        del Scene.mcprops


def collect_armatures(_, context):
    t = []
    for mod in context.object.modifiers:
        if mod.type != 'ARMATURE' or mod.object is None:
            continue
        name = mod.object.data.name
        t.append((name, name, ''))
    return t


class MeshProps(PropertyGroup):
    armature = EnumProperty(
        name="Armature",
        description="The armature that defines animations.",
        items=collect_armatures,
        options=set())
    uv_layer = StringProperty(
        name="UV Layer",
        description="The uv layer for texture mappings",
        options=set())
    artist = StringProperty(
        name="Artist name",
        description="Your name",
        options=set())
    render_groups = CollectionProperty(
        type=RenderGroups,
        name="Render Groups")
    active_render_group = IntProperty(
        name="Active Render Group",
        default=-1)
    default_group = PointerProperty(
        type=RenderGroups,
        name="Default Render Group",
        description="The group all faces are in if not in a valid group",
        options=set())

    @classmethod
    def register(cls):
        Mesh.mcprops = PointerProperty(type=MeshProps)

    @classmethod
    def unregister(cls):
        del Mesh.mcprops


class ActionProps(PropertyGroup):
    artist = StringProperty(
        name="Artist name",
        description="Your name",
        options=set())
    offset = IntProperty(
        name="Frame offset",
        description="The initial offset of the animation in frames",
        default=0,
        options=set())

    @classmethod
    def register(cls):
        Action.mcprops = PointerProperty(type=ActionProps)

    @classmethod
    def unregister(cls):
        del Action.mcprops


@persistent
def load_handler(_):
    # reset if loaded file is the default file
    if bpy.data.filepath == '':
        for scene in bpy.data.scenes:
            scene.mcprops.isuuidset = False


def register():
    bpy.app.handlers.load_post.append(load_handler)


def unregister():
    bpy.app.handlers.load_post.remove(load_handler)
