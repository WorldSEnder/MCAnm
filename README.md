Minecraft: Animated
====

Minecraft Animated created by WorldSEnder in an attempt to bring good looking
animations to Minecraft... for everyone.

Copyright(C)

This mod is a copyright and owned by WorldSEnder and has the right to own this
and to stop people for sharing it unlegalized.
Any violators shall be pursuit with demands of law.

1.) Do not post any of this without putting proper reference to this website
or the minecraftwiki page of this mod.

2.) Do not claim any of this to be your work unless authorized by one of it's
developers.

3.) Only developers of the MHFC modding group (https://github.com/Guild-Hall)
might alter and or/use altered code of this project without permission.

### Some words at the start

This readme is only for you if you yourself want to *make* a mod that uses
models and animations. If you only want to run it, install it like any other
minecraft mod with the jar
[from the download section](https://github.com/WorldSEnder/MCAnm/releases)

This project makes heavy use of the third party program
[Blender](http://www.blender.org) during the modeling and animation phase
although it is not bound to that. Blender is a "free and open source 3D
animation suite" without any fees or costs. You can get the latest version of
the softare [here](http://www.blender.org/download/).

Depending on your use of this mod you might have to install several plugins
that are part of the workflow. You can get those with this repository
[at the download section](https://github.com/WorldSEnder/MCAnm/releases).

### How to use

First you want to make your models and animations in Blender and export them
following the instructions on the wiki. It also offers references
(e.g. file specifications) for other modders/scripters to build on.

Models and animations will be present in binary form as .mcmd, .mcskl and
.mcanm files. You should use Loader-classes to load and later use them.
Example:

    ISkeleton skeleton = CommonLoader.loadSkeleton(filePath);
    IModel model = ClientLoader.loadModel(filePath, skeleton);
    IAnimation animation = CommonLoader.loadAnimation(filePath);


