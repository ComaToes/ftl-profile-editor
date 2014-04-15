ftl-profile-editor
==================

A 3rd-party tool to edit user files for [FTL](http://www.ftlgame.com/). It depends on resources from the game directory, but the game itself will not be modified.

With this, you can unlock any or all ships and achievements in your user profile, or tweak most aspects of saved games: crew, systems, weapons, fires, breaches, etc.

Note:
FTL 1.01-1.03.3's files are fully editable.
FTL 1.5.4's "ae_prof.sav" is editable, but its saved game is read-only.

<a href="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot05.png"><img src="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot05.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot04.png"><img src="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot04.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot02.png"><img src="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot02.png" width="145px" height="auto" /></a> &nbsp; <a href="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot03.png"><img src="https://raw.github.com/Vhati/ftl-profile-editor/master/img/screenshot03.png" width="145px" height="auto" /></a>

To download compiled binaries, [click here](https://sourceforge.net/projects/ftleditor/).

Comments can be made in a forum thread [here](http://www.ftlgame.com/forum/viewtopic.php?f=7&t=10959).

I can accept Paypal donations [via SourceForge](https://sourceforge.net/donate/?user_id=1569627).
That would be fantastic.


Usage
-----
* Exit FTL. The game must NOT be running.
* Double-click FTLProfileEditor.exe (Win) or FTLProfileEditor.command (Mac/Linux).
* On the first run, you may be prompted to locate your FTL data file. This is called "data.dat" in the "resources" directory under your FTL install. In most cases, this should be located automatically.
* Open a profile (ae_prof.sav or prof.sav) or saved game (continue.sav).
* Make any desired changes.
* Save, and close the editor.
* Fire up FTL and try out your new ship.


Requirements
------------
* Java (1.6 or higher).
    * http://www.java.com/en/download/
* FTL (1.01-1.03.3 or 1.5.4, Windows/OSX/Linux, Steam/GOG/Standalone).
    * http://www.ftlgame.com/
* WinXP SP1 can't run Java 1.7.
    * (1.7 was built with VisualStudio 2010, causing a DecodePointer error.)
    * To get 1.6, you may have to google "jdk-6u45-windows-i586.exe".


History
-------
This project forked after extended inactivity prevented the release of v12. The original codebase started by ComaToes can be found [here](https://github.com/ComaToes/ftl-profile-editor) and its associated forum thread is [here](http://www.ftlgame.com/forum/viewtopic.php?f=7&t=2877).
