package com.uniplore.job;

public class JobContext {
    private String id;
    private String name;
    private String user;
    private int priority;
    private String type;
    private String content;
    private int processId; // 进程ID
    private int executionTime; // 执行时间（秒）

    public JobContext(String id, String name, String user, int priority, String type, String content, int processId, int executionTime) {
        this.id = id;
        this.name = name;
        this.user = user;
        this.priority = priority;
        this.type = type;
        this.content = content;
        this.processId = processId;
        this.executionTime = executionTime;
    }

    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public int getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getProcessId() {
        return processId;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }
}
