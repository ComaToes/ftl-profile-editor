FTL Profile/SavedGame Editor
https://github.com/Vhati/ftl-profile-editor


About

  Also known as the "ComaToes Profile/SavedGame Editor", this is a
  3rd-party tool to edit user files. It depends on resources from the game
  directory, but the game itself will not be modified.

  With this, you can unlock any or all ships and achievements in your user
  profile, or tweak most aspects of saved games: crew, systems, weapons,
  fires, breaches, etc.

  Note:
  FTL 1.01-1.03.3's files are fully editable.
  FTL 1.5.4's "ae_prof.sav" is fully editable.
  FTL 1.5.4's saved game is only partially editable.


Requirements

  Java (1.6 or higher).
    http://www.java.com/en/download/

  FTL (1.01-1.03.3 or 1.5.4, Windows/OSX/Linux, Steam/GOG/Standalone).
    http://www.ftlgame.com/


Setup

  Extract the files from this archive anywhere.

  On the first run, you may be prompted to locate your
  FTL resources. Specifically "data.dat" in the "resources/"
  directory under your FTL install.

  On OSX, you can select "FTL.app", because the resources are inside it.

  In most cases, this should be located automatically.


Usage

  Exit FTL. The game must NOT be running.
  Double-click FTLProfileEditor.command.
  Switch to the appropriate tab: "Profile" or "Saved Game".
  Open a profile (ae_prof.sav or prof.sav) or saved game (continue.sav).
  Make any desired changes.
  Save, and close the editor.
  Fire up FTL and try out your new ship.


Troubleshooting

* If you get "java.lang.UnsupportedClassVersionError" on startup...
    You need a newer version of Java.

* Error reading profile. [...] Initial int not expected value: 2...
    You likely tried to open a saved game while in the "Profile" tab.

* Error reading saved game. [...] Unexpected first byte...
    You likely tried to open a profile while in the "Saved Game" tab.
