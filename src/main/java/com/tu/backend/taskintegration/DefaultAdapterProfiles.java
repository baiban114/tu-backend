package com.tu.backend.taskintegration;

public final class DefaultAdapterProfiles {

    private DefaultAdapterProfiles() {
    }

    public static String kaneo() {
        return """
            {
              "provider": "kaneo",
              "operations": {
                "listProjects": {
                  "method": "GET",
                  "path": "/api/project",
                  "query": { "workspaceId": "{{connection.workspaceId}}" },
                  "arrayPath": "data",
                  "fields": {
                    "id": ["id"],
                    "name": ["name", "title"],
                    "description": ["description"]
                  }
                },
                "listTasks": {
                  "method": "GET",
                  "path": "/api/task/tasks/{{projectId}}",
                  "query": {
                    "status": "{{status}}",
                    "priority": "{{priority}}",
                    "assigneeId": "{{assigneeId}}",
                    "page": "{{page}}",
                    "limit": "{{limit}}",
                    "sortBy": "{{sortBy}}",
                    "sortOrder": "{{sortOrder}}"
                  },
                  "arrayPath": "**tasks",
                  "fields": {
                    "id": ["id"],
                    "projectId": ["projectId", "project_id"],
                    "number": ["number", "key", "identifier"],
                    "title": ["title", "name"],
                    "description": ["description", "content"],
                    "status": ["status", "state", "column"],
                    "priority": ["priority"],
                    "assigneeName": ["assigneeName", "assignee_name", "assignee"],
                    "dueDate": ["dueDate", "due_date"],
                    "position": ["position", "order"],
                    "updatedAt": ["updatedAt", "updated_at", "modifiedAt"]
                  }
                },
                "createTask": {
                  "method": "POST",
                  "path": "/api/task/{{projectId}}",
                  "body": {
                    "title": "{{title}}",
                    "description": "{{description}}",
                    "projectId": "{{projectId}}",
                    "status": "{{status}}",
                    "priority": "{{priority}}",
                    "position": "{{position}}",
                    "assigneeId": "{{assigneeId}}",
                    "dueDate": "{{dueDate}}"
                  },
                  "defaults": { "description": "", "priority": "no-priority", "position": "0" }
                },
                "updateTask": {
                  "method": "PUT",
                  "path": "/api/task/{{taskId}}",
                  "body": {
                    "title": "{{title}}",
                    "description": "{{description}}",
                    "status": "{{status}}",
                    "priority": "{{priority}}",
                    "assigneeId": "{{assigneeId}}",
                    "dueDate": "{{dueDate}}"
                  }
                },
                "moveTask": {
                  "method": "PUT",
                  "path": "/api/task/status/{{taskId}}",
                  "body": {
                    "status": "{{status}}",
                    "columnId": "{{columnId}}",
                    "position": "{{position}}"
                  }
                }
              },
              "enumMappings": {
                "status": {
                  "todo": "to-do",
                  "doing": "in-progress",
                  "review": "in-review"
                }
              },
              "omitBlank": true
            }
            """;
    }
}
