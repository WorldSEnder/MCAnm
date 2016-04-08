import bpy

from bpy.types import Panel, Menu, UIList, Header, UILayout

from .operators import ObjectExporter, AnimationExporter,\
    AddRenderGroup, RemoveRenderGroup, AddFacesToGroup, SelectGroup,\
    UpdateGroupsVisual, TechneImport


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
            op = row.operator(AnimationExporter.bl_idname)
            # op.armature = context.object
            op.offset = sce.frame_start
            if not context.space_data.action:
                row.enabled = False
            else:
                op.arm_action = context.space_data.action.name


class AnimationExportHeader(Header):
    bl_label = "MCAnimation Export Menu"
    bl_idname = "animation.mc_export"
    bl_space_type = "DOPESHEET_EDITOR"

    def draw(self, context):
        layout = self.layout
        layout.menu(AnimationExportMenu.bl_idname)


class MeshDataPanel(Panel):
    # MC Panel under Object
    bl_idname = "object.mc_meshdata"
    bl_label = "Minecraft Animated"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "data"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'MESH'

    def draw(self, context):
        layout = LayoutWrapper(self.layout)
        data = context.object.data
        props = context.object.data.mcprops

        layout.prop(props, 'artist', text="Artist name").display()
        layout.prop(props, 'armature', text="Armature", icon="ARMATURE_DATA")\
            .display()
        layout.prop_search(
            props, 'uv_layer', data, 'uv_layers', text="UV Layer", icon="GROUP_UVS")\
            .add_test(lambda uv: uv, "Select a UV map.")\
            .add_test(lambda uv: uv in data.uv_layers, "Invalid UV map")\
            .display()
        box = layout.box()
        box.label(text="Default group")
        box.prop(props.default_group, 'name', text="Name")\
            .add_test(lambda n: n, "Select a default group name.")\
            .add_test(lambda n: n not in props.render_groups, "Name collision with existing group.")\
            .display()
        box.prop_search(props.default_group, 'image', bpy.data,
                        'images', text="Texture", icon="IMAGE_DATA")\
            .add_test(lambda img: img, "Select a default texture.")\
            .add_test(lambda img: img in bpy.data.images, "Invalid image texture")\
            .display()

        layout.separator()
        layout.label("Render Groups")
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
        groups = props.render_groups
        active_idx = props.active_render_group
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


class SceneDataPanel(Panel):
    # MC Panel under Object
    bl_idname = "scene.mcdata"
    bl_label = "Minecraft Animated"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "scene"

    def draw(self, context):
        layout = LayoutWrapper(self.layout)
        sceprops = context.scene.mcprops
        layout.prop(sceprops, 'projectname').display()


def export_func(self, context):
    self.layout.operator(
        ObjectExporter.bl_idname, text="Minecraft Animated models (.mcmd)")
    self.layout.operator(
        AnimationExporter.bl_idname, text="Minecraft Animations (.mcanm)")


def import_func(self, context):
    self.layout.operator(
        TechneImport.bl_idname, text="Techne Models (.tcn)")


def register():
    bpy.types.INFO_MT_file_export.append(export_func)
    bpy.types.INFO_MT_file_import.append(import_func)


def unregister():
    bpy.types.INFO_MT_file_export.remove(export_func)
    bpy.types.INFO_MT_file_import.remove(import_func)
