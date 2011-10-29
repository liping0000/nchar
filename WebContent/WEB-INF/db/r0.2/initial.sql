--
-- this file is generated with spy6
--


create table CHARMS_DOCUMENT (DOCUMENT_TYPE_ varchar2(31 char) not null, ID_ number(19,0) not null, COMMENT_ varchar2(2024 char), EDITOR_NAME_ varchar2(255 char), LAST_MODIFIED_ timestamp not null, MIME_TYPE_ varchar2(250 char) not null, NAME_ varchar2(250 char), SIZE_ number(19,0) not null, VERSION_ number(10,0), DOC_BLOB_ID_ number(19,0) unique, EDITOR_ID_ number(19,0), FOLDER_ID_ number(19,0), primary key (ID_))
create table CHARMS_DOC_BLB (ID_ number(19,0) not null, CONTENT_ blob not null, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_EMSG (ID_ number(19,0) not null, CONTENT_ clob, CREATE_ timestamp not null, KEY_ varchar2(255 char), RECEIVER_ varchar2(255 char) not null, SENDER_ varchar2(255 char) not null, SENT_ timestamp, SUBJECT_ varchar2(255 char) not null, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_ETMPL (ID_ number(19,0) not null, BODY_ clob, DESCRIPTION_ varchar2(2024 char), ENABLED_ char(1 char), LAST_MODIFIED_ timestamp not null, NAME_ varchar2(255 char), SENDER_ varchar2(255 char), SUBJECT_ varchar2(255 char), VERSION_ number(10,0), primary key (ID_))
create table CHARMS_ETMPL_RCVR (ID_ number(19,0) not null, ADDRESS_EXPR_ varchar2(500 char), VERSION_ number(10,0), TEMPLATE_ID_ number(19,0) not null, primary key (ID_))
create table CHARMS_ETMPL_TRANS (ID_ number(19,0) not null, BODY_ clob, LOCALE_ID_ varchar2(255 char) not null, SUBJECT_ varchar2(255 char), VERSION_ number(10,0), TEMPLATE_ID_ number(19,0) not null, primary key (ID_))
create table CHARMS_FOLDER (FOLDER_TYPE_ varchar2(31 char) not null, ID_ number(19,0) not null, COMMENT_ varchar2(255 char), NAME_ varchar2(255 char), VERSION_ number(10,0), BUSINESS_KEY_ varchar2(250 char), PROC_INST_ID_ number(19,0), PARENT_ID_ number(19,0), ROOT_ID_ number(19,0), primary key (ID_))
create table CHARMS_LOG (ID_ number(19,0) not null, DATE_ timestamp not null, LEVEL_ varchar2(10 char) not null, LOGGER_ varchar2(255 char) not null, MESSSAGE_ varchar2(1024 char) not null, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_MEMBERSHIP (ROLE_ID_ number(19,0) not null, USER_ID_ number(19,0) not null, primary key (ROLE_ID_, USER_ID_))
create table CHARMS_MESSAGES (MSG_BUNDLE_ID_ number(19,0) not null, VALUE_ varchar2(2500 char) not null, MSG_CODE_ varchar2(250 char) not null, primary key (MSG_BUNDLE_ID_, MSG_CODE_))
create table CHARMS_MSGBUNDLE (ID_ number(19,0) not null, LOCALE_ID_ varchar2(20 char) not null, NAME_ varchar2(100 char) not null, VERSION_ number(10,0), primary key (ID_), unique (NAME_, LOCALE_ID_))
create table CHARMS_PERMISSION (ID_ number(19,0) not null, ACTION_ varchar2(255 char) not null, DESCRIPTION_ varchar2(2024 char), DISCRIMINATOR_ varchar2(5 char) not null, RECIPIENT_ varchar2(15 char) not null, TARGET_ varchar2(120 char) not null, TARGET_ID_ number(19,0), VERSION_ number(10,0), primary key (ID_), unique (RECIPIENT_, DISCRIMINATOR_, TARGET_, TARGET_ID_))
create table CHARMS_PERM_TARGET (TARGET_TYPE_ varchar2(31 char) not null, ID_ number(19,0) not null, DESCRIPTION_ varchar2(2024 char), TARGET_STRING_ varchar2(255 char) not null unique, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_PROP_SET (ID_ number(19,0) not null, NAME_ varchar2(100 char) not null, SET_TYPE_ varchar2(5 char) not null, VERSION_ number(10,0), primary key (ID_), unique (NAME_, SET_TYPE_))
create table CHARMS_PROP_VAL (ID_ number(19,0) not null, NAME_ varchar2(100 char) not null, PROP_TYPE_ varchar2(20 char) not null, VALUE_ varchar2(2048 char), VERSION_ number(10,0), SET_ID_ number(19,0) not null, primary key (ID_), unique (NAME_, SET_ID_))
create table CHARMS_REPORT (ID_ number(19,0) not null, DEFAULT_NAME_ varchar2(50 char) not null unique, DESCRIPTION_ varchar2(2024 char), ENABLED_ char(1 char), UPDATE_ timestamp, MSG_CODE_ varchar2(255 char), SIZE_ number(19,0) not null, SORT_INDEX_ number(10,0) not null unique, VERSION_ number(10,0), REPORT_BLOB_ID_ number(19,0) unique, primary key (ID_))
create table CHARMS_ROLE (ID_ number(19,0) not null, GRP_ACTOR_ID_ varchar2(15 char) not null unique, CLASSIFICATION_ varchar2(20 char), CONDITIONAL_ char(1 char), DESCRIPTION_ varchar2(2024 char), LABEL_ varchar2(255 char), NAME_ varchar2(50 char) not null unique, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_RPT_BLB (ID_ number(19,0) not null, CONTENT_ blob not null, VERSION_ number(10,0), primary key (ID_))
create table CHARMS_TRGT_ACT (ID_ number(19,0) not null, DESCRIPTION_ varchar2(2024 char), NAME_ varchar2(255 char) not null, VERSION_ number(10,0), TARGET_ID_ number(19,0) not null, primary key (ID_), unique (NAME_, TARGET_ID_))
create table CHARMS_UID (SEQUENCE_TYPE_ varchar2(31 char) not null, ID_ number(19,0) not null, LAST_MODIFIED_ timestamp not null, VALUE_ varchar2(250 char) not null, VERSION_ number(10,0), LOCATION_ varchar2(255 char), PREFIX_ varchar2(255 char), SORT_INDEX_ varchar2(255 char), YEAR_ varchar2(255 char), primary key (ID_), unique (SEQUENCE_TYPE_, VALUE_))
create table CHARMS_USER (ID_ number(19,0) not null, ACCOUNT_EXPIRE_ timestamp, ACTOR_ID_ varchar2(15 char) not null unique, CREDENTIALS_EXPIRE_ timestamp, DESCRIPTION_ varchar2(2024 char), EMAIL_ varchar2(100 char), IS_ENABLED_ char(1 char), EXTERNAL_ID1_ varchar2(255 char), EXTERNAL_ID2_ varchar2(255 char), FIRSTNAME_ varchar2(60 char), GENDER_ varchar2(10 char), LABEL_ varchar2(255 char), LASTNAME_ varchar2(60 char), LOCALE_ID_ varchar2(100 char), NAME_ varchar2(20 char) not null unique, PASSWD_ varchar2(100 char), THEME_ID_ varchar2(100 char), TIMEZONE_ID_ varchar2(100 char), IS_UNLOCKED_ char(1 char), VERSION_ number(10,0), primary key (ID_))
create table CHARMS_WFL_DATA (ID_ number(19,0) not null, BUSINESS_KEY_ varchar2(250 char), FINISH_DATE_ timestamp, PRIORITY_ number(10,0), PROC_INST_ID_ number(19,0), SUBMIT_DATE_ timestamp, TITLE_ varchar2(250 char), VERSION_ number(10,0), FINISH_USER_ID_ number(19,0), SUBMIT_USER_ID_ number(19,0), primary key (ID_))
create table CHREQ_CODE (ID_ number(19,0) not null, DEFAULT_NAME_ varchar2(50 char) not null unique, ENABLED_ char(1 char), MSG_CODE_ varchar2(255 char), SORT_INDEX_ number(10,0) not null unique, VERSION_ number(10,0), primary key (ID_))
create table CHREQ_COSTSHEET (ID_ number(19,0) not null, BUSINESS_KEY_ varchar2(250 char), COMMENT_ varchar2(512 char), CONTENT_ clob not null, NAME_ varchar2(250 char), PROC_INST_ID_ number(19,0), SIZE_ number(19,0) not null, VERSION_ number(10,0), primary key (ID_))
create table CHREQ_DATA (ASSIGN_DATE_ timestamp, CONCLUSION_DESCRIPTION_ varchar2(2024 char), COST_A_ char(1 char), COST_AMOUNT_ number(10,0), COST_B_ char(1 char), CUSTOMER_NAME_ varchar2(255 char), FAST_TRACK_ char(1 char), IS_GOODWILL_ char(1 char), GOODWILL_TEXT_ varchar2(255 char), HISTORY_DESCRIPTION_ varchar2(2024 char), IMPLEMENT_DATE_ timestamp, INITIATE_DATE_ timestamp, ITEM_ID_NUMBER_ varchar2(255 char), MANAGER_NAME_ varchar2(100 char), MODULE_ID_NUMBER_ varchar2(255 char), PROBLEM_DESCRIPTION_ varchar2(2024 char), PROCESS_DATE_ timestamp, PROJECT_ID_NUMBER_ varchar2(50 char), PROPOSAL_DESCRIPTION_ varchar2(2024 char), REGULAR_TRACK_ char(1 char), IS_STANDARD_ char(1 char), STANDARD_TEXT_ varchar2(255 char), ID_ number(19,0) not null, ASSIGN_USER_ID_ number(19,0), CODE_ID_ number(19,0), PRODUCT_ID_ number(19,0), UNIT_ID_ number(19,0), IMPLEMENT_USER_ID_ number(19,0), INITIATE_USER_ID_ number(19,0), PROCESS_USER_ID_ number(19,0), primary key (ID_))
create table CHREQ_FACET_DATA (ID_ number(19,0) not null, CONCLUSION_MESSAGE_ varchar2(1024 char), FORWARD_DUE_DATE_ timestamp, FORWARD_MESSAGE_ varchar2(1024 char), FORWARD_ROLE_ID_ number(19,0), FORWARD_USER_ID_ number(19,0), HANDLE_DUE_DATE_ timestamp, HANDLE_MESSAGE_ varchar2(1024 char), HANDLE_ROLE_ID_ number(19,0), HANDLE_USER_ID_ number(19,0), HANDLED_MESSAGE_ varchar2(1024 char), IMPLEMENT_DUE_DATE_ timestamp, IMPLEMENT_MESSAGE_ varchar2(1024 char), IMPLEMENT_ROLE_ID_ number(19,0), IMPLEMENT_USER_ID_ number(19,0), IMPLEMENTED_MESSAGE_ varchar2(1024 char), PROCESS_DUE_DATE_ timestamp, PROCESS_MESSAGE_ varchar2(1024 char), PROCESS_ROLE_ID_ number(19,0), PROCESS_USER_ID_ number(19,0), REVIEW_DUE_DATE_ timestamp, REVIEW_MESSAGE_ varchar2(1024 char), REVIEW_ROLE_ID_ number(19,0), REVIEW_USER_ID_ number(19,0), REVIEWED_MESSAGE_ varchar2(1024 char), TID_ number(19,0) not null unique, VERSION_ number(10,0), VISIBLE_FACET_ varchar2(250 char), primary key (ID_))
create table CHREQ_IMPACTSHEET (ID_ number(19,0) not null, BUSINESS_KEY_ varchar2(250 char), COMMENT_ varchar2(512 char), CONTENT_ clob not null, NAME_ varchar2(250 char), PROC_INST_ID_ number(19,0), SIZE_ number(19,0) not null, VERSION_ number(10,0), primary key (ID_))
create table CHREQ_MSG_ENTRY (ID_ number(19,0) not null, AUTHOR_FULLNAME_ varchar2(121 char), AUTHOR_ID_ number(19,0), BUSINESS_KEY_ varchar2(250 char), CONTENT_ varchar2(2024 char), PROC_INST_ID_ number(19,0), RECEIVER_FULLNAME_ varchar2(121 char), RECEIVER_GRP_ID_ number(19,0), RECEIVER_GRPNAME_ varchar2(121 char), RECEIVER_ID_ number(19,0), TIMESTAMP_ timestamp, TITLE_ varchar2(250 char), MSG_TYPE_ varchar2(255 char), VERSION_ number(10,0), PARENT_ID_ number(19,0), IDX_ number(10,0), primary key (ID_))
create table CHREQ_PROD (ID_ number(19,0) not null, DEFAULT_NAME_ varchar2(50 char) not null unique, ENABLED_ char(1 char), MSG_CODE_ varchar2(255 char), SORT_INDEX_ number(10,0) not null unique, VERSION_ number(10,0), primary key (ID_))
create table CHREQ_PROD_CODES (PRODUCT_ID_ number(19,0) not null, CODE_ID_ number(19,0) not null, POSITION_ number(10,0) not null, primary key (PRODUCT_ID_, POSITION_), unique (PRODUCT_ID_, CODE_ID_))
create table CHREQ_PROD_UNITS (PRODUCT_ID_ number(19,0) not null, UNIT_ID_ number(19,0) not null, POSITION_ number(10,0) not null, primary key (PRODUCT_ID_, POSITION_), unique (PRODUCT_ID_, UNIT_ID_))
create table CHREQ_UNIT (ID_ number(19,0) not null, DEFAULT_NAME_ varchar2(50 char) not null unique, ENABLED_ char(1 char), MSG_CODE_ varchar2(255 char), SORT_INDEX_ number(10,0) not null unique, VERSION_ number(10,0), primary key (ID_))
create table JBPM4_DEPLOYMENT (DBID_ number(19,0) not null, NAME_ clob, TIMESTAMP_ number(19,0), STATE_ varchar2(255 char), primary key (DBID_))
create table JBPM4_DEPLOYPROP (DBID_ number(19,0) not null, DEPLOYMENT_ number(19,0), OBJNAME_ varchar2(255 char), KEY_ varchar2(255 char), STRINGVAL_ varchar2(255 char), LONGVAL_ number(19,0), primary key (DBID_))
create table JBPM4_EXECUTION (DBID_ number(19,0) not null, CLASS_ varchar2(255 char) not null, DBVERSION_ number(10,0) not null, ACTIVITYNAME_ varchar2(255 char), PROCDEFID_ varchar2(255 char), HASVARS_ number(1,0), NAME_ varchar2(255 char), KEY_ varchar2(255 char), ID_ varchar2(255 char) unique, STATE_ varchar2(255 char), SUSPHISTSTATE_ varchar2(255 char), PRIORITY_ number(10,0), HISACTINST_ number(19,0), INITIATOR_ varchar2(255 char), PARENT_ number(19,0), INSTANCE_ number(19,0), SUPEREXEC_ number(19,0), SUBPROCINST_ number(19,0), PARENT_IDX_ number(10,0), primary key (DBID_))
create table JBPM4_HIST_ACTINST (DBID_ number(19,0) not null, CLASS_ varchar2(255 char) not null, DBVERSION_ number(10,0) not null, HPROCI_ number(19,0), TYPE_ varchar2(255 char), EXECUTION_ varchar2(255 char), ACTIVITY_NAME_ varchar2(255 char), START_ timestamp, END_ timestamp, DURATION_ number(19,0), TRANSITION_ varchar2(255 char), NEXTIDX_ number(10,0), HTASK_ number(19,0), primary key (DBID_))
create table JBPM4_HIST_DETAIL (DBID_ number(19,0) not null, CLASS_ varchar2(255 char) not null, DBVERSION_ number(10,0) not null, USERID_ varchar2(255 char), TIME_ timestamp, HPROCI_ number(19,0), HPROCIIDX_ number(10,0), HACTI_ number(19,0), HACTIIDX_ number(10,0), HTASK_ number(19,0), HTASKIDX_ number(10,0), HVAR_ number(19,0), HVARIDX_ number(10,0), MESSAGE_ clob, OLD_STR_ varchar2(255 char), NEW_STR_ varchar2(255 char), OLD_INT_ number(10,0), NEW_INT_ number(10,0), OLD_TIME_ timestamp, NEW_TIME_ timestamp, PARENT_ number(19,0), PARENT_IDX_ number(10,0), primary key (DBID_))
create table JBPM4_HIST_PROCINST (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, ID_ varchar2(255 char), PROCDEFID_ varchar2(255 char), KEY_ varchar2(255 char), START_ timestamp, END_ timestamp, DURATION_ number(19,0), STATE_ varchar2(255 char), ENDACTIVITY_ varchar2(255 char), SUPERPROCID_ varchar2(255 char), INITIATOR_ varchar2(255 char), NEXTIDX_ number(10,0), primary key (DBID_))
create table JBPM4_HIST_TASK (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, NAME_ varchar2(255 char), EXECUTION_ varchar2(255 char), OUTCOME_ varchar2(255 char), ASSIGNEE_ varchar2(255 char), PRIORITY_ number(10,0), STATE_ varchar2(255 char), CREATE_ timestamp, END_ timestamp, DURATION_ number(19,0), DUEDATE_ timestamp, NEXTIDX_ number(10,0), PROCINST_ number(19,0), SUPERTASK_ number(19,0), primary key (DBID_))
create table JBPM4_HIST_VAR (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, PROCINSTID_ varchar2(255 char), EXECUTIONID_ varchar2(255 char), VARNAME_ varchar2(255 char), VALUE_ varchar2(255 char), HPROCI_ number(19,0), HTASK_ number(19,0), primary key (DBID_))
create table JBPM4_JOB (DBID_ number(19,0) not null, CLASS_ varchar2(255 char) not null, DBVERSION_ number(10,0) not null, DUEDATE_ timestamp, STATE_ varchar2(255 char), ISEXCLUSIVE_ number(1,0), LOCKOWNER_ varchar2(255 char), LOCKEXPTIME_ timestamp, EXCEPTION_ clob, RETRIES_ number(10,0), PROCESSINSTANCE_ number(19,0), EXECUTION_ number(19,0), CFG_ number(19,0), SIGNAL_ varchar2(255 char), EVENT_ varchar2(255 char), REPEAT_ varchar2(255 char), primary key (DBID_))
create table JBPM4_LOB (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, BLOB_VALUE_ blob, DEPLOYMENT_ number(19,0), NAME_ varchar2(255 char), primary key (DBID_))
create table JBPM4_PARTICIPATION (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, GROUPID_ varchar2(255 char), USERID_ varchar2(255 char), TYPE_ varchar2(255 char), TASK_ number(19,0), SWIMLANE_ number(19,0), primary key (DBID_))
create table JBPM4_PROPERTY (KEY_ varchar2(255 char) not null, VERSION_ number(10,0) not null, VALUE_ varchar2(255 char), primary key (KEY_))
create table JBPM4_SWIMLANE (DBID_ number(19,0) not null, DBVERSION_ number(10,0) not null, NAME_ varchar2(255 char), ASSIGNEE_ varchar2(255 char), EXECUTION_ number(19,0), primary key (DBID_))
create table JBPM4_TASK (DBID_ number(19,0) not null, CLASS_ char(1 char) not null, DBVERSION_ number(10,0) not null, NAME_ varchar2(255 char), DESCR_ varchar2(4000 char), STATE_ varchar2(255 char), SUSPHISTSTATE_ varchar2(255 char), ASSIGNEE_ varchar2(255 char), FORM_ varchar2(255 char), PRIORITY_ number(10,0), CREATE_ timestamp, DUEDATE_ timestamp, PROGRESS_ number(10,0), SIGNALLING_ number(1,0), EXECUTION_ID_ varchar2(255 char), ACTIVITY_NAME_ varchar2(255 char), HASVARS_ number(1,0), SUPERTASK_ number(19,0), EXECUTION_ number(19,0), PROCINST_ number(19,0), SWIMLANE_ number(19,0), TASKDEFNAME_ varchar2(255 char), primary key (DBID_))
create table JBPM4_VARIABLE (DBID_ number(19,0) not null, CLASS_ varchar2(255 char) not null, DBVERSION_ number(10,0) not null, KEY_ varchar2(255 char), CONVERTER_ varchar2(255 char), HIST_ number(1,0), EXECUTION_ number(19,0), TASK_ number(19,0), LOB_ number(19,0), DATE_VALUE_ timestamp, DOUBLE_VALUE_ double precision, CLASSNAME_ varchar2(255 char), LONG_VALUE_ number(19,0), STRING_VALUE_ varchar2(255 char), TEXT_VALUE_ clob, EXESYS_ number(19,0), primary key (DBID_))
create table TODOL_DATA (TASK_DESCRIPTION_ varchar2(2024 char), ID_ number(19,0) not null, primary key (ID_))


create table CHARMS_DOCUMENT add constraint FK74FA867E65F35154 foreign key (EDITOR_ID_) references CHARMS_USER
create table CHARMS_DOCUMENT add constraint FK74FA867E36732CF6 foreign key (DOC_BLOB_ID_) references CHARMS_DOC_BLB
create table CHARMS_DOCUMENT add constraint FK74FA867EDAD4E6F6 foreign key (FOLDER_ID_) references CHARMS_FOLDER
create table CHARMS_ETMPL_RCVR add constraint FK1A3A75254AE5E2 foreign key (TEMPLATE_ID_) references CHARMS_ETMPL
create table CHARMS_ETMPL_TRANS add constraint FK34FC5C0254AE5E2 foreign key (TEMPLATE_ID_) references CHARMS_ETMPL
create table CHARMS_FOLDER add constraint FK7CFD29123DF16A foreign key (ROOT_ID_) references CHARMS_FOLDER
create table CHARMS_FOLDER add constraint FK7CFD291FD9A5532 foreign key (PARENT_ID_) references CHARMS_FOLDER

create table CHARMS_MEMBERSHIP add constraint FK1236C79B2D29888 foreign key (ROLE_ID_) references CHARMS_ROLE
create table CHARMS_MEMBERSHIP add constraint FK1236C79B2D5BCB2 foreign key (USER_ID_) references CHARMS_USER
create table CHARMS_MESSAGES add constraint FK2612B6AF63D6AEF1 foreign key (MSG_BUNDLE_ID_) references CHARMS_MSGBUNDLE
create table CHARMS_PROP_VAL add constraint FK6777188C6DADD71 foreign key (SET_ID_) references CHARMS_PROP_SET
create table CHARMS_REPORT add constraint FK1BBF0B5731F8FD1 foreign key (REPORT_BLOB_ID_) references CHARMS_RPT_BLB
create table CHARMS_TRGT_ACT add constraint FK988D6F81F46918A6 foreign key (TARGET_ID_) references CHARMS_PERM_TARGET

create table CHARMS_WFL_DATA add constraint FK5E7D28AFA80948F9 foreign key (SUBMIT_USER_ID_) references CHARMS_USER
create table CHARMS_WFL_DATA add constraint FK5E7D28AFEE5AD6DE foreign key (FINISH_USER_ID_) references CHARMS_USER
create table CHREQ_DATA add constraint FKBA395630C90BD99C foreign key (PRODUCT_ID_) references CHREQ_PROD
create table CHREQ_DATA add constraint FKBA39563084BA3798 foreign key (INITIATE_USER_ID_) references CHARMS_USER
create table CHREQ_DATA add constraint FKBA395630E4C7B434 foreign key (UNIT_ID_) references CHREQ_UNIT
create table CHREQ_DATA add constraint FKBA395630C2750086 foreign key (CODE_ID_) references CHREQ_CODE
create table CHREQ_DATA add constraint FKBA395630A7B2B84E foreign key (IMPLEMENT_USER_ID_) references CHARMS_USER
create table CHREQ_DATA add constraint FKBA395630674B0A02 foreign key (ASSIGN_USER_ID_) references CHARMS_USER
create table CHREQ_DATA add constraint FKBA39563040679C82 foreign key (PROCESS_USER_ID_) references CHARMS_USER
create table CHREQ_DATA add constraint FKBA39563049EAE22 foreign key (ID_) references CHARMS_WFL_DATA
create table CHREQ_MSG_ENTRY add constraint FKDFC3872E6AF4F41F foreign key (PARENT_ID_) references CHREQ_MSG_ENTRY
create table CHREQ_PROD_CODES add constraint FK37C6EB24C90BD99C foreign key (PRODUCT_ID_) references CHREQ_PROD
create table CHREQ_PROD_CODES add constraint FK37C6EB24C2750086 foreign key (CODE_ID_) references CHREQ_CODE
create table CHREQ_PROD_UNITS add constraint FK38C4326DC90BD99C foreign key (PRODUCT_ID_) references CHREQ_PROD
create table CHREQ_PROD_UNITS add constraint FK38C4326DE4C7B434 foreign key (UNIT_ID_) references CHREQ_UNIT
create index IDX_DEPLPROP_DEPL on JBPM4_DEPLOYPROP (DEPLOYMENT_)
create table JBPM4_DEPLOYPROP add constraint FK_DEPLPROP_DEPL foreign key (DEPLOYMENT_) references JBPM4_DEPLOYMENT
create index IDX_EXEC_SUBPI on JBPM4_EXECUTION (SUBPROCINST_)
create index IDX_EXEC_PARENT on JBPM4_EXECUTION (PARENT_)
create index IDX_EXEC_SUPEREXEC on JBPM4_EXECUTION (SUPEREXEC_)
create index IDX_EXEC_INSTANCE on JBPM4_EXECUTION (INSTANCE_)
create table JBPM4_EXECUTION add constraint FK_EXEC_SUBPI foreign key (SUBPROCINST_) references JBPM4_EXECUTION
create table JBPM4_EXECUTION add constraint FK_EXEC_INSTANCE foreign key (INSTANCE_) references JBPM4_EXECUTION
create table JBPM4_EXECUTION add constraint FK_EXEC_SUPEREXEC foreign key (SUPEREXEC_) references JBPM4_EXECUTION
create table JBPM4_EXECUTION add constraint FK_EXEC_PARENT foreign key (PARENT_) references JBPM4_EXECUTION
create index IDX_HTI_HTASK on JBPM4_HIST_ACTINST (HTASK_)
create index IDX_HACTI_HPROCI on JBPM4_HIST_ACTINST (HPROCI_)
create table JBPM4_HIST_ACTINST add constraint FK_HACTI_HPROCI foreign key (HPROCI_) references JBPM4_HIST_PROCINST
create table JBPM4_HIST_ACTINST add constraint FK_HTI_HTASK foreign key (HTASK_) references JBPM4_HIST_TASK
create index IDX_HDET_HVAR on JBPM4_HIST_DETAIL (HVAR_)
create index IDX_HDET_HACTI on JBPM4_HIST_DETAIL (HACTI_)
create index IDX_HDET_HTASK on JBPM4_HIST_DETAIL (HTASK_)
create index IDX_HDET_HPROCI on JBPM4_HIST_DETAIL (HPROCI_)
create table JBPM4_HIST_DETAIL add constraint FK_HDETAIL_HVAR foreign key (HVAR_) references JBPM4_HIST_VAR
create table JBPM4_HIST_DETAIL add constraint FK_HDETAIL_HPROCI foreign key (HPROCI_) references JBPM4_HIST_PROCINST
create table JBPM4_HIST_DETAIL add constraint FK_HDETAIL_HTASK foreign key (HTASK_) references JBPM4_HIST_TASK
create table JBPM4_HIST_DETAIL add constraint FK_HDETAIL_HACTI foreign key (HACTI_) references JBPM4_HIST_ACTINST
create index IDX_HSUPERT_SUB on JBPM4_HIST_TASK (SUPERTASK_)
create table JBPM4_HIST_TASK add constraint FK_HSUPERT_SUB foreign key (SUPERTASK_) references JBPM4_HIST_TASK
create index IDX_HVAR_HTASK on JBPM4_HIST_VAR (HTASK_)
create index IDX_HVAR_HPROCI on JBPM4_HIST_VAR (HPROCI_)
create table JBPM4_HIST_VAR add constraint FK_HVAR_HPROCI foreign key (HPROCI_) references JBPM4_HIST_PROCINST
create table JBPM4_HIST_VAR add constraint FK_HVAR_HTASK foreign key (HTASK_) references JBPM4_HIST_TASK
create index IDX_JOBRETRIES on JBPM4_JOB (RETRIES_)
create index IDX_JOBDUEDATE on JBPM4_JOB (DUEDATE_)
create index IDX_JOBLOCKEXP on JBPM4_JOB (LOCKEXPTIME_)
create index IDX_JOB_CFG on JBPM4_JOB (CFG_)
create index IDX_JOB_EXE on JBPM4_JOB (EXECUTION_)
create index IDX_JOB_PRINST on JBPM4_JOB (PROCESSINSTANCE_)
create table JBPM4_JOB add constraint FK_JOB_CFG foreign key (CFG_) references JBPM4_LOB
create index IDX_LOB_DEPLOYMENT on JBPM4_LOB (DEPLOYMENT_)
create table JBPM4_LOB add constraint FK_LOB_DEPLOYMENT foreign key (DEPLOYMENT_) references JBPM4_DEPLOYMENT
create index IDX_PART_TASK on JBPM4_PARTICIPATION (TASK_)
create table JBPM4_PARTICIPATION add constraint FK_PART_SWIMLANE foreign key (SWIMLANE_) references JBPM4_SWIMLANE
create table JBPM4_PARTICIPATION add constraint FK_PART_TASK foreign key (TASK_) references JBPM4_TASK
create index IDX_SWIMLANE_EXEC on JBPM4_SWIMLANE (EXECUTION_)
create table JBPM4_SWIMLANE add constraint FK_SWIMLANE_EXEC foreign key (EXECUTION_) references JBPM4_EXECUTION
create index IDX_TASK_SUPERTASK on JBPM4_TASK (SUPERTASK_)
create table JBPM4_TASK add constraint FK_TASK_SWIML foreign key (SWIMLANE_) references JBPM4_SWIMLANE
create table JBPM4_TASK add constraint FK_TASK_SUPERTASK foreign key (SUPERTASK_) references JBPM4_TASK
create index IDX_VAR_EXESYS on JBPM4_VARIABLE (EXESYS_)
create index IDX_VAR_TASK on JBPM4_VARIABLE (TASK_)
create index IDX_VAR_EXECUTION on JBPM4_VARIABLE (EXECUTION_)
create index IDX_VAR_LOB on JBPM4_VARIABLE (LOB_)
create table JBPM4_VARIABLE add constraint FK_VAR_EXESYS foreign key (EXESYS_) references JBPM4_EXECUTION
create table JBPM4_VARIABLE add constraint FK_VAR_LOB foreign key (LOB_) references JBPM4_LOB
create table JBPM4_VARIABLE add constraint FK_VAR_TASK foreign key (TASK_) references JBPM4_TASK
create table JBPM4_VARIABLE add constraint FK_VAR_EXECUTION foreign key (EXECUTION_) references JBPM4_EXECUTION
create table TODOL_DATA add constraint FK5B1E838349EAE22 foreign key (ID_) references CHARMS_WFL_DATA
create table hibernate_sequences ( sequence_name varchar2(255 char) not null ,  next_val number(19,0), primary key ( sequence_name ) ) 
