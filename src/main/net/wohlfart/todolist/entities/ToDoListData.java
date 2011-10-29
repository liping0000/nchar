package net.wohlfart.todolist.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.wohlfart.framework.entities.CharmsWorkflowData;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.validator.Length;



/**
 * this class implements a simple human task
 * 
 * 
 * @author Michael Wohlfart
 */
@Indexed
@Entity
@Table(name = "TODOL_DATA")
public class ToDoListData extends CharmsWorkflowData implements Serializable {

    // max length of the textfields
    public static final int MAX_CONTENT_LENGTH = 2024;

    // the richtext components
    private String taskDescription;

    @Length(max = MAX_CONTENT_LENGTH)
    @Column(name = "TASK_DESCRIPTION_", length = MAX_CONTENT_LENGTH)
    public String getTaskDescription() {
        return taskDescription;
    }
    public void setTaskDescription(final String taskDescription) {
        this.taskDescription = taskDescription;
    }

}
