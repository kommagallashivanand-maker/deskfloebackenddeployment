import logging
from contextvars import ContextVar

correlation_id_ctx_var: ContextVar[str] = ContextVar("correlation_id", default="")


class CorrelationIdFormatter(logging.Formatter):
    def format(self, record):
        corr_id = correlation_id_ctx_var.get()
        record.correlation_id = corr_id if corr_id else "-"
        return super().format(record)


def get_logger(name: str = __name__):
    logger = logging.getLogger(name)
    if not logger.handlers:
        handler = logging.StreamHandler()
        formatter = CorrelationIdFormatter(
            "%(asctime)s - %(levelname)s - %(name)s - [CID: %(correlation_id)s] - %(message)s"
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)
    logger.setLevel(logging.INFO)
    return logger
