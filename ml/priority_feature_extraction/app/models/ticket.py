from typing import List, Optional
from pydantic import BaseModel


class AttachmentMeta(BaseModel):
    filename: str
    content_type: Optional[str]
    size: Optional[int]


class TicketIn(BaseModel):
    subject: Optional[str] = ""
    description: Optional[str] = ""
    category: Optional[str] = None
    attachments: Optional[List[AttachmentMeta]] = None


class AuthContext(BaseModel):
    user_id: str
    role: Optional[str] = None
