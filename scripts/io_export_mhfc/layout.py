import bpy

from bpy.types import Panel, Menu, UIList, Header, UILayout

from .operators import ObjectExporter, ArmAnimationExporter, ArmatureUpdater,\
    AddRenderGroup, RemoveRenderGroup, AddFacesToGroup, SelectGroup,\
    UpdateGroupsVisual, ImportTechne


class LayoutWrapper(object):
    _layout = None

    def __init__(self, layout):
        self._layout = layout

    def __dir__(self):
        return dir(self._layout)

    def __getattr__(self, name):
        if name in {"row", "column", "column_flow", "box", "split"}:
            fn = getattr(self._layout, name)

            def wrapped(*args, **wargs):
                sublayout = fn(*args, **wargs)
                return LayoutWrapper(sublayout)
            return wrapped
        if name in {"prop", "props_enum", "prop_menu_enum", "prop_enum",
                    "prop_search", "template_ID", "template_ID_preview",
                    "template_any_ID", "template_path_builder",
                    "template_curve_mapping", "template_color_ramp",
                    "template_icon_view", "template_histogram",
                    "template_waveform", "template_vectorscope",
                    "template_layers", "template_color_picker",
                    "template_image", "template_movieclip",
                    "template_track", "template_marker",
                    "template_movieclip_information", "template_component_menu",
                    "template_colorspace_settings",
                    "template_colormanaged_view_settings"}:
            def wrapped(data, prop, *args, **wargs):
                wrapper = self

                class BoundArgs(object):
                    _tests = []

                    def add_test(self, predicate, error_message):
                        self._tests.append((predicate, error_message))
                        return self

                    def display(self):
                        row_layout = wrapper._layout.row()
                        original_call = getattr(
                            row_layout, name)(data, prop, *args, **wargs)
                        errored = False
                        for pre, mess in self._tests:
                            if not pre(getattr(data, prop)):
                                wrapper._layout.label(mess, icon="ERROR")
                                errored = True
                        if errored:
                            row_layout.label(icon="ERROR")
                        return original_call

                return BoundArgs()
            return wrapped
        prop = getattr(self._layout, name)
        if callable(prop):
            def wrapped(*args, **wargs):
                return prop(*args, **wargs)
            return wrapped
        return prop


class RenderGroupUIList(UIList):

    def draw_item(self, context, layout, data, item, icon, active_data, active_propname):
        if self.layout_type in {'DEFAULT', 'COMPACT'}:
            if item:
                row = layout.row()
                row.prop(item, "name", text="", emboss=False, icon_value=icon)
                if item.image not in bpy.data.images:
                    row.label(icon="ERROR")
            else:
                layout.label(text="", translate=False, icon_value=icon)
        # 'GRID' layout type should be as compact as possible (typically a single icon!).
        elif self.layout_type in {'GRID'}:
            layout.alignment = 'CENTER'
            layout.label(text="", icon_value=icon)


class AnimationExportMenu(Menu):
    bl_label = "Export Action"
    bl_idname = "animation.mc_export"

    def draw(self, context):
        layout = self.layout
        sce = context.scene
        if context.space_data.mode == 'ACTION':
            row = layout.row()
            op = row.operator(ArmAnimationExporter.bl_idname)
            # op.armature = context.object
            op.offset = sce.frame_start
            if not context.space_data.action:
                row.enabled = False
            else:
                op.arm_action = context.space_data.action.name


class AnimationExportHeader(Header):
    bl_label = "Animation Export Menu"
    bl_idname = "animation.mc_export"
    bl_space_type = "DOPESHEET_EDITOR"

    def draw(self, context):
        layout = self.layout
        layout.menu(AnimationExportMenu.bl_idname)


class ObjectPropertiesPanel(Panel):
    # MC Panel under Object
    bl_label = "Monster Hunter Frontier Craft"
    bl_idname = "object.mc_export"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "object"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'MESH'

    def draw(self, context):
        # setup vars
        obj = context.object
        data = obj.data
        meshprops = data.mcprops
        layout = LayoutWrapper(self.layout)
        # check for invalidity: #poll
        # Properties of this object
        box = layout
        box.prop(meshprops, 'artist', text="Artist name").display()
        box.prop(meshprops, "name", text="Model Name")\
            .add_test(lambda k: k, "Select model name").display()
        box.prop_search(meshprops, 'armature', meshprops,
                        'poss_arms', text="Armature", icon="ARMATURE_DATA")\
            .add_test(lambda a: not a or a in meshprops.poss_arms, "Invalid Armature")\
            .display()
        box.operator(ArmatureUpdater.bl_idname)
        box.prop_search(
            meshprops, 'uv_layer', data, 'uv_layers', text="UV Layer", icon="GROUP_UVS")\
            .add_test(lambda uv: uv, "Select a UV map.")\
            .add_test(lambda uv: uv in data.uv_layers, "Invalid UV map")\
            .display()
        box = box.box()
        box.label(text="Default group")
        box.prop(meshprops.default_group, 'name', text="Name")\
            .add_test(lambda n: n, "Select a default group name.")\
            .add_test(lambda n: n not in meshprops.render_groups, "Name collision with existing group.")\
            .display()
        box.prop_search(meshprops.default_group, 'image', bpy.data,
                        'images', text="Texture", icon="IMAGE_DATA")\
            .add_test(lambda img: img, "Select a default texture.")\
            .add_test(lambda img: img in bpy.data.images, "Invalid image texture")\
            .display()


class MeshDataPanel(Panel):
    # MC Panel under Object
    bl_idname = "object.mc_meshdata"
    bl_label = "Minecraft Render Groups"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "data"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'MESH'

    def draw(self, context):
        layout = LayoutWrapper(self.layout)
        props = context.object.data.mcprops
        groups = props.render_groups
        active_idx = props.active_render_group

        row = layout.row()
        row.template_list(
            listtype_name='RenderGroupUIList',
            dataptr=props,
            propname='render_groups',
            active_dataptr=props,
            active_propname='active_render_group',
            rows=2,
            type='DEFAULT',
        )
        col = row.column(align=True)
        col.operator(AddRenderGroup.bl_idname, text="", icon='ZOOMIN')
        col.operator(RemoveRenderGroup.bl_idname, text="", icon='ZOOMOUT')
        if active_idx >= 0 and active_idx < len(groups):
            active_g = groups[active_idx]
            layout.prop_search(active_g, 'image', bpy.data, 'images', text="Image", icon="IMAGE_DATA")\
                .add_test(lambda img: img, "Select an imagetexture for active group")\
                .add_test(lambda img: img in bpy.data.images, "Invalid image texture")\
                .display()
        if context.mode == 'EDIT_MESH':
            row = layout.row()
            row.operator(AddFacesToGroup.bl_idname)
            row.operator(SelectGroup.bl_idname)
            layout.operator_menu_enum(UpdateGroupsVisual.bl_idname, 'mode')


class AnimationExportPanel(Panel):
    # MC Panel under Object
    bl_space_type = "VIEW_3D"
    bl_region_type = "TOOLS"
    bl_category = "Export"
    bl_label = "Minecraft"
    bl_idname = "view3d.mcanm_export"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'ARMATURE'

    def draw(self, context):
        layout = self.layout
        obj = context.object
        sce = context.scene
        row = layout.row()
        row.operator_context = 'INVOKE_DEFAULT'
        op = row.operator(ArmAnimationExporter.bl_idname)
        op.offset = sce.frame_start
        if obj.animation_data is not None and obj.animation_data.action is not None:
            action = obj.animation_data.action
            op.arm_action = action.name
            op.armature = obj.name
            layout.prop(action.mcprops, 'artist')
            op.artist = action.mcprops.artist
        else:
            row.enabled = False


def export_func(self, context):
    self.layout.operator(
        ObjectExporter.bl_idname, text="Minecraft Animated models (.mcmd)")


def import_func(self, context):
    self.layout.operator(
        ImportTechne.bl_idname, text="Techne Models (.tcn)")


def register():
    bpy.types.INFO_MT_file_export.append(export_func)
    bpy.types.INFO_MT_file_import.append(import_func)


def unregister():
    bpy.types.INFO_MT_file_export.remove(export_func)
    bpy.types.INFO_MT_file_import.remove(import_func)
