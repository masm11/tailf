# What is Tailf?

UNIX tail command supports the -f option.
It watches the file modification once per 1sec,
and output appended data to the screen.

Recent Linux distributions has tailf command.
It receives file modification event with inotify system calls
instead of watching the file once per 1sec,
and output appended data to the screen.

This Tailf app displays appended data on the screen,
like Linux's tailf command.

# Environments

I run on the following environments:

  - Xperia X Performance
  - Android 6.0.1

I think the Tailf app doesn't run on older version of android.

Also, about SD cards,
I don't think the Tailf app can handle them on other than
Xperia X Performance.

# Installation

I don't put the app on Google Play, so you need to build and install
the app on Android Studio.

The app uses the following permission.

  - android.permission.READ_EXTERNAL_STORAGE

# Usage

Tap a button on the top-right corder, and tap the "Open..." in the menu.

Please select a file.

The selected file is open, and its contents is displayed.
When the file is appended, the screen is updated.

The app has 1000 lines from the end of the file, so you can scroll.

When you select a file, you can select any file (even if it is
ther than a text file) but whether the app can open it is another problem.
Also, even if the app can open it, it is not displayed normally
if it is an image file or such.

# Invoke from other apps

You can invoke the Tailf app from other apps like File Manager.

When you try to open a text file, the Tailf app is included in
the list of available apps. You can select Tailf.

You cannot use Tailf if the file isn't recognized as a text file.

# "File System"

You can select a provider "File System", with which you can see
all the files and directories you can see.
But there are many files and directories the app can't open
even if you can see them.

You don't have to get root since the app do only things
it can without root.

# Bugs

Android Marshmallow has the critical bug, so I don't use FileObserver.
The app watches a file once per 1sec.

# License

GPL3. See COPYING.

# Author

Yuuki Harano &lt;masm@masm11.ddo.jp&gt;
