from pytm import TM, Actor, Process, Dataflow, Datastore, Boundary, Data, Classification

tm = TM("Refund Handling")
tm.description = "Threat model for handling refund requests through direct API calls to a NodeJS backend and a database."
tm.isOrdered = True

# Boundaries
backend_boundary = Boundary("Backend (NodeJS API)")
database_boundary = Boundary("Database")

# Actors
support_staff = Actor("Support Staff")
support_staff.description = "Support staff responsible for handling refund requests through API requests."

# Processes
refund_controller = Process("RefundController")
refund_controller.inBoundary = backend_boundary
refund_controller.description = "Controller that handles refund requests."

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
    description="Refund request details such as refund ID and action.",
    classification=Classification.SENSITIVE,
    isPII=False,
    isStored=True,
    isSourceEncryptedAtRest=True,
    isDestEncryptedAtRest=True,
)

refund_status = Data(
    name="Refund Status",
    description="Status of the refund request after processing.",
    classification=Classification.SENSITIVE,
    isPII=False,
    isStored=False,
)

# Simplified Dataflows
Dataflow(support_staff, refund_controller, "PATCH /api/refunds/{id} (API Request)").protocol = "HTTPS"

df1 = Dataflow(refund_controller, refund_service, "Process refund request")
df1.protocol = "Internal API"
df1.data = refund_data

df2 = Dataflow(refund_service, refund_repository, "Update refund status in database")
df2.protocol = "Internal API"

df3 = Dataflow(refund_repository, refund_requests_table, "Query/Update refund request")
df3.protocol = "SQL"
df3.dstPort = 3306

df4 = Dataflow(refund_requests_table, refund_repository, "Return update confirmation")
df4.protocol = "SQL"
df4.data = refund_status

df5 = Dataflow(refund_repository, refund_service, "Return data to service layer")
df5.protocol = "Internal API"
df5.data = refund_status

df6 = Dataflow(refund_service, refund_controller, "Return processed refund status")
df6.protocol = "Internal API"
df6.data = refund_status

df7 = Dataflow(refund_controller, support_staff, "200 OK + refund status (API Response)")
df7.protocol = "HTTPS"
df7.data = refund_status

# Process the threat model
tm.process()