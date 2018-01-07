Changelog

26:
- Fixed a crash when editing shops
- Fixed a crash when saving a saved game with missiles/asteroids/explosions
- Fixed "Ship" tab doors not repainting after open/closed changes are applied
- Fixed "Ship" tab selector overlay not filling visible area to catch mouse events
- Fixed "Ship" tab crew mid-walk being placed at their destination
- Fixed "Ship" tab battle/repair drone bodies losing their move goals upon saving
- Moved "General" tab's Sector and Boss panels into the "Sector Map" tab
- Fixed "General" tab's Encounter panel not disabling for old saved game formats
- Fixed Crew Records not listing all AE races when editing ("General Stats" tab)
- Fixed Achievements' dump text repeating 'With Type-B' value for Type-C
- Fixed launcher scripts/executable for Java 9 (NoClassDefFoundError: javax/xml/bind)
- Fixed hidden files not shown when locating FTL resources
- Fixed unresolved symlinks when locating FTL resources
- Changed the config file's keys from camelCase to underscores
- Added RNG-informed previews for sector map layouts
- Added a warning when attempting to dump the blank default profile
- Added alternate locations to locate FTL resources on Linux
- Added 'FTLProfileEditor_debug.bat' for troubleshooting on Windows
- Changed logging framework to SLF4J/Logback
- Migrated to the Apache HttpComponents library
- Made launcher script on OSX find java the recommended way
- Made update checking optional and less frequent

25:
- Reinstated nearby ship stealing in saved games ("Change Ship" tab)
- Improved sector tree editing in saved games ("Sector Tree" tab)
- Added top score removal in profiles
- Added hangar's newbie tip level in profiles ("General Achievements" tab)
- Added current event editing in saved games ("General" tab)
- Fixed fire placement (Previously, rows/cols were mixed up)
- Fixed misreported difficulty in old "profile.sav" scores
- Fixed FTLProfileEditor.command double-clicking on linux?

24:
- Added support for FTL 1.5.12/1.5.13 saved games
- Added Quest/Victory achievement editing in profiles ("Ship Stats" tab)
- Fixed a bug that omitted the Fed Cruiser Quest achievement in profiles
- Fixed slow mouse wheel scrolling in side panels of "Ship" and "Sector Map" tabs

23:
- Partial ship editing for 1.5.4 saved games (systems/drones/weapons excluded)
- Fixed an FTL crash caused by newly-created stores not starting with a shelf
- Fixed first-run notice, to say saved games are partially editable
- Fixed inaccurate skill mastery interval hints in saved game dumps
- Fixed State Var "higho2" tooltip: FTL 1.5.4 incremented on arrival/loading
- Fixed Lanius ship achievements leaking into old "prof.sav" files
- Added lossy "ae_prof.sav" editing when using resources from FTL 1.01-1.03.3
- Added a warning when attempting to save the blank default profile

22:
- Partial editing for 1.5.4 saved games (ships themselves are not editable yet
- Added "Ship Best" score editing in profiles
- Added backups when overwriting a profile or saved game
- Fixed read error when a ship's system spans multiple rooms (boss artillery)
- Fixed bug that always reset the first beacon when a saved game was saved

21:
- Major saved game parsing improvements, thanks to many samples from users
- Added warning nags for nonsensical actions
- Fixed a null exception when some typical resources are missing
- Fixed errors when using resources from FTL 1.03.3 and earlier
- Fixed a profile read error when there's a Normal/Hard victory achievement
- Improved bug report dialogs

20:
- Fixed profile read error when there are victories by Engi/Fed/Lanius ships
- Added a WIP notice when run the first time
- Added pastable bug reports that embed the problematic files in question

19:
- Added read/write support for FTL 1.5.4 "ae_prof.sav" profiles
- Added profile "Dump" tab
- Added read-only support for FTL 1.5.4 saved games (unstable)
- Updated log4j2 to 2.0-beta9, fixing a hang when run with Java 1.7.0_25

18:
- Fixed bug that required 'running as admin' on Windows
- Fixed bug that prevented saving a ship's "Reserve Power Capacity"

17:
- Added platform-specific launchers to double-click instead of the jar
- Added readmes
- Cleaned up some dodgy code when initially prompting for FTL's location
- Incorporated code from Slipstream Mod Manager
- Revised the initial stats of ships spawned by the "Change Ship" tab
- Allowed DataManager subclasses to be set as the global instance

16:
- Added profile score editing.

15:
- Added sector number rollback under the "General" tab
- Added "State Vars" tab
- Added automated finding of data.dat for OSX-Steam
- Added a prompt to override automatically found data.dat location
- Fixed manual data.dat choosing on OSX. (Thanks to wilerson)
- Allowed negative background sprite rotation for SectorMap beacons

14:
- Added cargo editing under the "General" tab
- Added "In Hidden Sector" field under the "General" tab
- Added map-related rebel flagship fields under the "General" tab
- All beacon fields are editable
- The unknown field in visited beacons became background sprite rotation

13:
- Revised how FTL's data.dat is located: cfg contains ftlDatsPath=.../(FTL dir)/resources/
- Added automated finding of data.dat for Linux-Steam, and possibly OSX
- Fixed crash when writing a saved game in which a store had been visited that sector
- Added SectorMap editing: Store, Quest, and Beacon (partial)

12:
- The saved game editor can tweak nearly all aspects of a player's ship and crew
- The profile editor can unlock at specific difficulties

11:
- Added saved game (continue.sav) parsing.
- Some general ship attributes (hull/fuel/scrap/etc) are editable
- Fires, breaches and oxygen levels can be reset
- Fixed crash when Type B Crystal ship appears in high scores

10:
- Stats tab now shows all stats (Session/Crew/Totals areas were previously blank)
- FTL data is now accessed without needing to unpack the data files
- Older versions of this tool will have unpacked to a folder called "ftldata" - You should delete this to free up disk space
- Added a toolbar button to unpack data files
- Added release notes viewer

9:
- Added correct parsing of achievement difficulty flag (this fixes a profile parser error where you had attained an achievement at normal difficulty)

8:
- Added general achievements and top scores
- Modified parser error dialog to contain text to copy/paste into bug report

7:
- Fixed Linux bug (thanks roostertech)
- Added error logging to file (ftl-profile-editor.log)
- Added default profile locations for MacOS and Linux

6:
- Now uses FTL data files to extract ship/achievement data and images

5:
- Added a check when opening a profile to ensure the app can read and write it without losing data

4:
- Added automatic update checking

3:
- Bug fixes

2:
- Bug fixes

1:
- Initial release
