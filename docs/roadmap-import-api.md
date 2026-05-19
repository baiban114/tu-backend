# Roadmap Import API

## Generate Knowledge Base From Roadmap JSON

- Method: `POST`
- Path: `/api/kbs/import-roadmap`

Behavior:

- Creates the knowledge base, page tree, and each page's initial `richtext` block in one transaction.
- Roadmap node title supports `title` or `name`; child nodes use `children`.
- Initial page content uses node `content` first, then `description`.
- Request accepts `root`, `pages`, or `roadmap` containing an object or an array.
- One import is limited to 500 pages.

Request:

```json
{
  "name": "Java Learning Roadmap",
  "icon": "📚",
  "roadmap": {
    "title": "Java",
    "description": "From syntax to engineering practice",
    "children": [
      {
        "title": "Basics",
        "children": [
          { "title": "Variables and Types" },
          { "title": "Control Flow" }
        ]
      }
    ]
  }
}
```

Response:

```json
{
  "knowledgeBase": {
    "id": "kb-1",
    "name": "Java Learning Roadmap",
    "icon": "📚",
    "description": null
  },
  "pages": [],
  "pageCount": 4
}
```
