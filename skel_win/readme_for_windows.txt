FTL Profile/SavedGame Editor
https://github.com/Vhati/ftl-profile-editor


About

  Also known as the "ComaToes Profile/SavedGame Editor", this is a
  3rd-party tool to edit user files. It depends on resources from the game
  directory, but the game itself will not be modified.

  With this, you can unlock any or all ships and achievements in your user
  profile, or tweak most aspects of saved games: crew, systems, weapons,
  fires, breaches, etc.


Status

  FTL 1.6.1+ profiles are not supported at all.
  FTL 1.5.4-1.5.13 profiles are fully editable. (ae_prof.sav)
  FTL 1.01-1.03.3 profiles are fully editable. (prof.sav)

  FTL 1.6.1+ saved games are not supported at all.
  FTL 1.5.4-1.5.13 saved games are partially editable.
  FTL 1.01-1.03.3 saved games are fully editable.


Requirements

  Java (1.6 or higher).
    http://www.java.com/en/download/

  FTL (1.01-1.03.3 or 1.5.4-1.5.13, Windows/OSX/Linux, Steam/GOG/Standalone).
    https://subsetgames.com/

  * WinXP SP1 can't run Java 1.7.
    (1.7 was built with VisualStudio 2010, causing a DecodePointer error.)
    To get 1.6, you may have to google "jdk-6u45-windows-i586.exe".


Setup

  Extract the files from this archive anywhere.

  On the first run, you may be prompted to locate your
  FTL resources. Specifically "data.dat" in the "resources\"
  directory under your FTL install.

  In most cases, this should be located automatically.


Usage

  Quit FTL to before editing profiles.

  For saved games, you must NOT be actively playing a campaign.
    FTL 1.5.4+: The main menu is safe.
    FTL 1.01-1.03.3: "Save+Quit".

  Double-click FTLProfileEditor.exe.
  Switch to the appropriate tab: "Profile" or "Saved Game".
  Open a profile (ae_prof.sav or prof.sav) or saved game (continue.sav).
  Make any desired changes, and save.

  Continue playing FTL.


Troubleshooting

* General advice...
    Double-click FTLProfileEditor_debug.bat.
    That will show you the logs and offer to fix interface glitches.

* If you get "java.lang.UnsupportedClassVersionError" on startup...
    You need a newer version of Java.

* If you get "java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException"...
    That should only occur if you try to run "java -jar ..." yourself.
    Java 9 made one of its libraries opt-in, requiring a special argument.

    java --add-modules=java.xml.bind -jar FTLProfileEditor.jar

    The launcher straddles Java releases by telling older ones to ignore it.

    java -XX:+IgnoreUnrecognizedVMOptions ...

* If you get "NullPointerException" on startup (at com.sun.java.swing.plaf)...
    You may have set unusual theme for your OS, and Java's having trouble
    getting all the info it expects in order to mimic the aesthetics.

    Tell the editor to use the default Java UI instead of the native theme.
    Edit or create "ftl-editor.cfg" with notepad, in the editor's folder,
    with the following line.

    use_default_ui=true

* Error reading profile. [...] Initial int not expected value: 2...
    You likely tried to open a saved game while in the "Profile" tab.

* Error reading saved game. [...] Unexpected first byte...
    You likely tried to open a profile while in the "Saved Game" tab.
