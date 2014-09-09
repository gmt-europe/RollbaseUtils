# Rollbase Utilities

LGPL License.

## Introduction

The Rollbase Utilities project provides utilities for working with Rollbase. This project consists of a number of
sub projects, which provide different functions.

## Rollbase XSD Schema

The rollbase-shared project contains an XSD schema for Rollbase XML Application exports. Besides the XSD schema, the
rollbase-shared export also contains generated JAXB classes for working with the Rollbase XML Application exports.
These classes can be used to load a schema into memory, modify the application and save it back.

## Rollbase Merge

The rollbase-merge project provides functions for integrating Rollbase into a version control system.

The purpose of this project is to transform the Rollbase XML Application exports into something that works well with
a version control system. It accomplishes this in two ways:

* The large, monolithic XML files that Rollbase generates can be split into multiple separate files. These files are
  stored in a hierarchy that matches the types of the objects. This makes working with them easier. You only focus
  on a specific XML file, e.g. the file of an Object or Web page;

* The internal ID's that Rollbase generates are transformed into UUID's. The reason for doing this is that this ID's
  are not unique over different databases. As part of this transformation, ID's are also rewritten. Every object
  contains an ID and an original ID. The original ID is the ID of the object that stays the same when imported and
  exported into different databases. The normal ID of the object is local to the database the export was created from.
  To make the XML files more stable (i.e. not having a lot of changes every time you do an export), the ID's are made
  the same as their associated original ID's.

To make use of the rollbase-merge project, you need to follow a specific workflow. These steps are important to make
sure that no conflicts arise while working with the rollbase-merge tool.

### Database per branch

A branch defines a specific state of the application. To make sure that Rollbase is synchronized with the branch,
a database must be created per branch. One of the ways this can be accomplished is by scripting this. This would work
as follows:

* When you create a new branch, you create a new database. This can either be an empty database, or a copy of the
  database the branch was created from. Say ou create a topic branch from the development branch, you can copy the
  database of the development branch to a new database with the name of the topic branch;

* Next, Rollbase needs to reference the new database. This again can be scripted. A way to accomplish this would be
  to create a script that takes a database name, changes the conf/databases.xml file and restarts Tomcat.

When creating or copying a database, the internal ID table must be modified. Internal ID's are taken from the RB_ID
table. When you create a copy, the value of the record with ID_TYPE 1 should be incremented with some amount, e.g.
1,000,000. The primary reason for doing this is to work around a bug in Rollbase. Objects in Rollbase (not the
tables, but really every kind of element in Rollbase, including e.g. Fields and Charts) have both an ID and an
original ID. The original ID of these objects must be unique for the application over different databases. When an
object is created in a database, the original ID is set to the ID assigned in that database. If care is not taken, it
is possible that the original ID created in this fashion conflicts with an original ID created in a different database.
To work around this problem, every database must have their own range of ID's it uses to generate ID's and original
ID's. The range that is used per database needs to be synchronized with other team members to ensure that there are
no overlapping ID ranges for any database created. Rollbase supports having ID's created by a central instance of the
router application. Instructions for working in this way still have to be created.

### Version control workflow

The basic version control workflow is described below. This workflow uses two functions of the rollbase-merge project:

* The "load" function loads a Rollbase project and creates an XML Application export from it that you can import into
  Rollbase;

* The "save" function saves an XML Application export created by Rollbase into the version control repository.

The workflow is as follows:

* You start out by creating an export from the current application and creating a new version control project from ths
  using the "save" function. This creates the initial version of the application in version control;

* Branching works as usual. You can just create a branch in the version control repository and mirror that branch
  by creating a new database;

* After you've done some work, you create an export of the application and store this into the branch using the "save"
  function. This will update the version control project with your changes;

* Merging the branch into the main branch requires a few steps. First, the branch needs to be merged as usual. You can
  use your normal tools for this. However, once the merge is completed, two steps need to be taken to make sure the
  version control project stays valid:
  
  * The "appId" property of the Application.xml file needs to be checked. This UUID identifies the application version
    on a branch. By default, a merge would overwrite this with the "appId" of the branch that was merged. This needs to
    be reset to the value the "appId" had on the branch you merged into;
  
  * Second, the branch you merged into needs to get an updated version of the application. First execute the "load"
    function to generate an XML Application export from the current state of the project. This action may update the
    Application.xml and IdMaps.xml files, if new objects were introduced because of the merge. These changes need to be
    checked in;
  
  * An optional step is to create an export from the Rollbase instance you just imported the application into. The
    reason for doing this is that Rollbase may apply a few fixes to the merged version of the application.

Specifically the merge step of this process is a bit involved. The merge process may create up to two new commits.
It is not required that these commits stay separate commits. If you are e.g. using Git, you can amend both these commits
into the merge commit, since they really are artifacts created because of the merge process.

### Application ID

Every time you save your project using the "save" command, a new application ID is created. This ID is added to
the IdMaps.xml file and is used to keep track of the mapping between Rollbase ID's and GUID's that RollbaseUtils
creates. This application ID needs to be synchronized with your Rollbase application. This means that after you
update the exported project using the "save" command, you need to issue a "load" command to re-created the
XML export from the current state of the project. This file then needs to be imported back into Rollbase. This
will make sure that the application ID in Rollbase is in sync with the application ID in the IdMaps.xml file
in your export project.

# Issues

This project currently is in alpha state and there will be issues. If you find any problems with the libraries or
tools provided by this project, please create a new issue in the
[issues section](https://github.com/gmt-europe/RollbaseUtils/issues).
