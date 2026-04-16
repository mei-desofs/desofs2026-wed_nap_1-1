from pytm import TM, Actor, Process, Dataflow, Datastore, Boundary, Data, Classification

tm = TM("View Refund Requests")
tm.description = "Threat model for viewing refund requests through direct API calls to a NodeJS backend and a database."
tm.isOrdered = True

# Boundaries
backend_boundary = Boundary("Backend (NodeJS API)")
database_boundary = Boundary("Database")

# Actors
support = Actor("Support")
support.description = "Support staff responsible for viewing refund requests through API requests."

# Processes
refund_controller = Process("RefundController")
refund_controller.inBoundary = backend_boundary
refund_controller.description = "Controller that handles requests to view refunds."

refund_service = Process("RefundService")
refund_service.inBoundary = backend_boundary
refund_service.description = "Service layer for business logic related to refunds."

refund_repository = Process("RefundRepository")
refund_repository.inBoundary = backend_boundary
refund_repository.description = "Repository layer for database access."

# Datastore
refund_requests_table = Datastore("RefundRequests Table")
refund_requests_table.inBoundary = database_boundary
refund_requests_table.description = "Database table storing refund request information."
refund_requests_table.isSql = True
refund_requests_table.isHardened = True

# Data
refund_data = Data(
    name="Refund Data",
    description="Refund request details such as refund ID, status, and reason.",
    classification=Classification.SENSITIVE,
    isPII=False,
    isStored=True,
    isSourceEncryptedAtRest=True,
    isDestEncryptedAtRest=True,
)

# Simplified Dataflows
Dataflow(support, refund_controller, "GET /api/refunds (API Request)").protocol = "HTTPS"

df1 = Dataflow(refund_controller, refund_service, "Fetch refund requests")
df1.protocol = "Internal API"

df2 = Dataflow(refund_service, refund_repository, "Query refund requests from database")
df2.protocol = "Internal API"

df3 = Dataflow(refund_repository, refund_requests_table, "Query refund requests")
df3.protocol = "SQL"
df3.dstPort = 3306

df4 = Dataflow(refund_requests_table, refund_repository, "Return refund data")
df4.protocol = "SQL"
df4.data = refund_data

df5 = Dataflow(refund_repository, refund_service, "Return refund data to service layer")
df5.protocol = "Internal API"
df5.data = refund_data

df6 = Dataflow(refund_service, refund_controller, "Return refund data to controller")
df6.protocol = "Internal API"
df6.data = refund_data

df7 = Dataflow(refund_controller, support, "200 OK + refund data (API Response)")
df7.protocol = "HTTPS"
df7.data = refund_data

# Process the threat model
tm.process()