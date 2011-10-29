
-- manual sql updates for the next Charms Release --

-------------------------------------
-- drop all foreign key contraints --
-------------------------------------
select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
from user_constraints where CONSTRAINT_TYPE = 'R';

--------------------------------
-- drop all unique contraints --
--------------------------------

select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
from user_constraints where CONSTRAINT_TYPE = 'U';



-- ================ snip ======================= --
 ----------------------------------------
 -- update the charms role/user tables --
 ----------------------------------------

 -- CHARMS_INCL_ROLES 
create table CHARMS_INCL_ROLES (MAIN_ID_ number(19,0) not null, INCL_ID_ number(19,0) not null, primary key (MAIN_ID_, INCL_ID_), unique (INCL_ID_, MAIN_ID_));
commit;
alter table CHARMS_INCL_ROLES add constraint FK_CIROLES01 foreign key (INCL_ID_) references CHARMS_ROLE;
alter table CHARMS_INCL_ROLES add constraint FK_CIROLES02 foreign key (MAIN_ID_) references CHARMS_ROLE;
commit;

 -- CHARMS_UPSTRM_ROLES
create table CHARMS_UPSTRM_ROLES (DOWN_ID_ number(19,0) not null, UP_ID_ number(19,0) not null, primary key (DOWN_ID_, UP_ID_), unique (UP_ID_, DOWN_ID_));
commit;
alter table CHARMS_UPSTRM_ROLES add constraint FK_CUROLES01 foreign key (UP_ID_) references CHARMS_ROLE;
alter table CHARMS_UPSTRM_ROLES add constraint FK_CUROLES02 foreign key (DOWN_ID_) references CHARMS_ROLE;
commit;

-- changeing the role table
alter table CHARMS_ROLE add CLASSIFICATION_ VARCHAR2(20 char);
alter table CHARMS_ROLE drop column ORGANIZATIONAL_;
commit;

-- new table columns:
alter table CHARMS_ROLE add LABEL_ VARCHAR2(255 CHAR);
update CHARMS_ROLE set LABEL_ = NAME_;
alter table CHARMS_USER add LABEL_ VARCHAR2(255 CHAR);
update CHARMS_USER set LABEL_ = FIRSTNAME_ || ' ' || LASTNAME_;

 ----------------------------------------
 --   creating the transition tables   --
 ----------------------------------------

 -- CHARMS_TRNS_CHOICE
create table CHARMS_TRNS_CHOICE (ID_ number(19,0) not null, SELECTED_ varchar2(255 char), TASK_DBID_ number(19,0), VERSION_ number(10,0), primary key (ID_));
commit;

 -- CHARMS_TRNS_DATA
create table CHARMS_TRNS_DATA (ID_ number(19,0) not null, DUE_DATE_ timestamp, MESSAGE_ varchar2(2024 char), RECEIVER_LABEL_ varchar2(255 char), REMIND_DATE_ timestamp, REMIND_INTERVAL_ varchar2(255 char), TRANSITION_NAME_ varchar2(255 char), VERSION_ number(10,0), RCVR_GRP_ID_ number(19,0), RCVR_USR_ID_ number(19,0), SWIMLANE_ID_ number(19,0), CHOICE_ID_ number(19,0), primary key (ID_));
commit;
alter table CHARMS_TRNS_DATA add constraint FK_CTDATA01 foreign key (RCVR_USR_ID_) references CHARMS_USER;
alter table CHARMS_TRNS_DATA add constraint FK_CTDATA02 foreign key (RCVR_GRP_ID_) references CHARMS_ROLE;
alter table CHARMS_TRNS_DATA add constraint FK_CTDATA03 foreign key (CHOICE_ID_) references CHARMS_TRNS_CHOICE;
alter table CHARMS_TRNS_DATA add constraint FK_CTDATA04 foreign key (SWIMLANE_ID_) references JBPM4_SWIMLANE;
commit;

 -- adding group data to the message entry
alter table CHREQ_MSG_ENTRY add RECEIVER_GRPNAME_ VARCHAR2(121 CHAR);
alter table CHREQ_MSG_ENTRY add RECEIVER_GRP_ID_ NUMBER(19,0);
commit;



 ----------------------------------------
 --   creating the revision table      --
 ----------------------------------------

 -- the revision system
create table CHARMS_REVISION (ID_ number(19,0) not null, TIMESTAMP_ number(19,0), USER_NAME_ varchar2(255 char), primary key (ID_));
commit;



 ----------------------------------------
 -- fields from the jbpm 4.4 release   --
 ----------------------------------------

 -- jbpm4 updates
alter table JBPM4_EXECUTION add INITIATOR_ VARCHAR2(255 CHAR);
alter table JBPM4_HIST_PROCINST add INITIATOR_ VARCHAR2(255 CHAR);
alter table JBPM4_HIST_PROCINST add SUPERPROCID_ VARCHAR2(255 CHAR);
commit;





 --------------------------------------------------
 -- adding a foreign key in the permission table --
 --------------------------------------------------

 -- optional foreign key in CHARMS_PERMISSION
alter table CHARMS_PERMISSION add TARGET_ID_ NUMBER(19,0);
alter table CHARMS_PERMISSION add constraint IDX_CPERMISSN01 unique (RECIPIENT_, DISCRIMINATOR_, TARGET_, TARGET_ID_);
 -- check the old constaint and delete constraint:
 --select * from user_constraints where table_name='CHARMS_PERMISSION';
-- manual: alter table CHARMS_PERMISSION drop constraint SYS_C0042793;
commit;


 -----------------
 -- table fixes --
 -----------------

 -- renaming PROP_TYPE_ to SET_TYPE_
alter table CHARMS_PROP_SET add SET_TYPE_ VARCHAR2(20 CHAR);
update CHARMS_PROP_SET set SET_TYPE_ = PROP_TYPE_ where 1 = 1;
alter table CHARMS_PROP_SET add constraint IDX_CPSET01 unique (NAME_, SET_TYPE_);
 -- check the old constaint and delete column & constraint:
 --select * from user_constraints where table_name='CHARMS_PROP_SET';
-- manual: alter table CHARMS_PROP_SET drop constraint SYS_C0042802;
commit;

-- type for properties
alter table CHARMS_PROP_VAL add PROP_TYPE_ VARCHAR2(20 char) default 'DATE' not null;
commit;

-- deprecated table
drop table CHARMS_CHILDROLES;


-- modify datatypes:
alter table CHARMS_EMSG MODIFY (KEY_ VARCHAR2(255 CHAR)); 
update CHARMS_PERMISSION set DISCRIMINATOR_='role' where DISCRIMINATOR_ like 'role%';
alter table CHARMS_PERMISSION MODIFY (DISCRIMINATOR_ VARCHAR2(5 CHAR)); 
alter table CHARMS_PERMISSION MODIFY (RECIPIENT_ VARCHAR2(50 CHAR)); 
alter table CHARMS_PERMISSION MODIFY (TARGET_ VARCHAR2(120 CHAR)); 
-- manual: alter table JBPM4_TASK MODIFY (DESCR_ VARCHAR2(4000 CHAR)); 


---------------------
-- leftover fixes  --
---------------------

-- datatype of the name column changed from clob to varchar2(255)
alter table JBPM4_LOB add NEW_NAME_ VARCHAR2(255 CHAR); 
update JBPM4_LOB set NEW_NAME_ = NAME_ where 1=1;
alter table JBPM4_LOB drop column NAME_;
alter table JBPM4_LOB rename column NEW_NAME_ to NAME_;

alter table CHARMS_PROP_SET drop column PROP_TYPE_;

alter table CHARMS_PROP_SET modify SET_TYPE_ not null;

alter table CHARMS_TRGT_ACT modify (NAME_ VARCHAR2(255 CHAR) not null);

alter table JBPM4_TASK drop column DESCR_;
alter table JBPM4_TASK add DESCR_ VARCHAR2(255 CHAR);



-- ================ snip ======================= --
    

-------------------------------------
--    drop all constraints again   --
-------------------------------------
select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
  from user_constraints where CONSTRAINT_TYPE = 'R';

select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
  from user_constraints where CONSTRAINT_TYPE = 'P';
  
select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
  from user_constraints where CONSTRAINT_TYPE = 'U';
  
select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
  from user_constraints where CONSTRAINT_TYPE = 'U';

select 'drop index ' || INDEX_NAME || ';'
  from user_indexes where INDEX_TYPE = 'NORMAL';
 
  
  --> upon restart copy&paste the new process definition...
  
--__________________________________________________________________________  
  
-- delete all (finished) execution variables
delete from JBPM4_VARIABLE where EXECUTION_ in (select DBID_ from JBPM4_EXECUTION where STATE_ = 'ended');
delete from JBPM4_SWIMLANE where 1=1;
delete from JBPM4_VARIABLE where 1=1;
-- delete changelogs...
delete from DATABASECHANGELOG where 1=1;
  
  new valid entry for and execution/process instance:
  
  DBID_ CLASS_  DBVERSION_  KEY_    CONVERTER_  HIST_   EXECUTION_  TASK_   LOB_    DATE_VALUE_ DOUBLE_VALUE_   CLASSNAME_  LONG_VALUE_ STRING_VALUE_   TEXT_VALUE_ EXESYS_ 
1093    hib-long    0   changeRequestCostSheet  <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestCostSheet  181 <NULL>  <NULL>  <NULL>  
1096    hib-long    0   changeRequestData   <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestData   181 <NULL>  <NULL>  <NULL>  
1098    hib-long    0   changeRequestFolder <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestFolder 181 <NULL>  <NULL>  <NULL>  
1097    hib-long    0   messageEntry    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestMessageEntry   1470    <NULL>  <NULL>  <NULL>  
1095    hib-long    0   changeRequestImpactSheet    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestImpactSheet    181 <NULL>  <NULL>  <NULL>  
1094    hib-long    0   changeRequestMessageTree    <NULL>  0   425 <NULL>  <NULL>  <NULL>  <NULL>  net.wohlfart.changerequest.entities.ChangeRequestMessageEntry   1470    <NULL>  <NULL>  <NULL>  

--__________________________________________________________________________  
 

-- to list all constraints
select * from user_constraints;


------  foreign key constraints:

-- to list all foreign key constraints:
select A.CONSTRAINT_NAME,
       A.TABLE_NAME,
       B.TABLE_NAME
   from user_constraints A join user_constraints B on A.R_CONSTRAINT_NAME = B.CONSTRAINT_NAME 
where A.CONSTRAINT_TYPE = 'R';

-- to rename a contraint:
ALTER TABLE CHARMS_TRNS_DATA RENAME CONSTRAINT FKE9B89CA35B710AFB TO FK_TRNSD_SWIML;

-- generate rename scripts:
select 'alter table ' || A.TABLE_NAME || ' rename constraint ' || A.CONSTRAINT_NAME || ' to ' || 'FK_',
       A.TABLE_NAME,
       B.TABLE_NAME
   from user_constraints A join user_constraints B on A.R_CONSTRAINT_NAME = B.CONSTRAINT_NAME 
where A.CONSTRAINT_TYPE = 'R';


------- primary key constraints:

-- generate rename scripts:
select 'alter table ' || TABLE_NAME || ' rename constraint ' || CONSTRAINT_NAME || ' to ' || 'FK_' || TABLE_NAME || ';'
  from user_constraints where CONSTRAINT_TYPE = 'P';

  
------- indices:

  select * from user_indexes;

select 'alter index ' || I.INDEX_NAME || ' rename to IDX_' || C.TABLE_NAME,
  from user_indexes I join user_constraints C on I.INDEX_NAME = C.CONSTRAINT_NAME 
  where I.INDEX_TYPE = 'NORMAL';
  

-- to list all unique constraints
select * from user_constraints where 1 = 1;

select *
from user_constraints where CONSTRAINT_TYPE = 'R';


select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
from user_constraints where CONSTRAINT_TYPE = 'R';

select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
from user_constraints where CONSTRAINT_TYPE = 'C';

select 'alter table ' || TABLE_NAME || ' drop constraint ' || CONSTRAINT_NAME || ';'
from user_constraints where CONSTRAINT_TYPE = 'P';



