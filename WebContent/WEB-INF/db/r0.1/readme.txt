rolling this update still requires some manual interaction:

- make sure the CAMS user has alter table permission:

- check the hibernate sequence

- run the liquibase diff script/ant task

manual changes after the diff:

alter table CHREQ_DATA
drop CONSTRAINT FKBA395630E324318E

alter table CHREQ_PROD_ERRORS
drop CONSTRAINT FKC4B0F40DE324318E;

alter table CHREQ_PROD_ERRORS
drop CONSTRAINT FKC4B0F40DC90BD99C;

alter table CHREQ_ERROR
drop CONSTRAINT SYS_C0011790;

alter table CHREQ_ERROR
drop CONSTRAINT SYS_C0011791;

alter table CHREQ_DATA
drop column ERROR_ID_

alter table CHARMS_PROP_SET
drop constraint SYS_C0011740

drop table CHREQ_ERROR

drop table CHREQ_PROD_ERRORS

checks:

select e.ID_,
e.DEFAULT_NAME_, c.DEFAULT_NAME_,
e.SORT_INDEX_, c.SORT_INDEX_
from CHREQ_ERROR e, CHREQ_CODE c
where e.ID_ = c.ID_



reset after update:



