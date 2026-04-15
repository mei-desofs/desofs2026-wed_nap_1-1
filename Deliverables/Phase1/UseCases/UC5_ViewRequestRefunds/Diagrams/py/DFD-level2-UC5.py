from pytm import TM, Actor, Process, Dataflow, Datastore, Boundary, Data, Classification

tm = TM("View Refund Requests")
tm.description = "Simplified threat model for viewing refund requests in a React SPA with a NodeJS backend and a database."
tm.isOrdered = True

# Boundaries
frontend_boundary = Boundary("Frontend (React SPA)")
backend_boundary = Boundary("Backend (NodeJS API)")
database_boundary = Boundary("Database")

# Actors
support = Actor("Support")
support.description = "Support staff responsible for viewing refund requests."

# Processes
refunds_view = Process("Refunds View")
refunds_view.inBoundary = frontend_boundary
refunds_view.description = "Frontend page for viewing refund requests."

api_client = Process("API Client")
api_client.inBoundary = frontend_boundary
api_client.description = "Frontend client that communicates with the backend API."

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
Dataflow(support, refunds_view, "Clicks 'View Refunds'").protocol = "HTTPS"

df1 = Dataflow(refunds_view, api_client, "Send request to API")
df1.protocol = "HTTPS"

df2 = Dataflow(api_client, refund_controller, "Forward request to backend")
df2.protocol = "HTTPS"

df3 = Dataflow(refund_controller, refund_service, "Fetch refund requests")
df3.protocol = "Internal API"

df4 = Dataflow(refund_service, refund_repository, "Query refund requests from database")
df4.protocol = "Internal API"

df5 = Dataflow(refund_repository, refund_requests_table, "Query refund requests")
df5.protocol = "SQL"
df5.dstPort = 3306

df6 = Dataflow(refund_requests_table, refund_repository, "Return refund data")
df6.protocol = "SQL"
df6.data = refund_data

df7 = Dataflow(refund_repository, refund_service, "Return refund data to service layer")
df7.protocol = "Internal API"
df7.data = refund_data

df8 = Dataflow(refund_service, refund_controller, "Return refund data to controller")
df8.protocol = "Internal API"
df8.data = refund_data

df9 = Dataflow(refund_controller, api_client, "Return response to client")
df9.protocol = "HTTPS"
df9.data = refund_data

df10 = Dataflow(api_client, refunds_view, "Display refund data to support")
df10.protocol = "HTTPS"
df10.data = refund_data

# Process the threat model
tm.process()