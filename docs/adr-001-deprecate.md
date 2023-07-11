## Proposal - Deprecate AMIable

Author: @NovemberTang

Date: 2023-07-11

### Abstract
AMIable is a web app for monitoring number of instances running out of date
AMIs. This ADR proposes to deprecate Amiable and replace it with a cloudquery
view.

### Background

#### Reasons to move to Cloudquery
- AMIable is not widely used across the department
- AMIable has a maintenance burden (eg. updating dependencies). This is a
burden on the DevX team. We are already responsible for maintaining cloudquery
so this has no additional maintenance cost except for perhaps occasionally
modifying the cloudquery view.
- AMIable does not provide a complete view of out of date AMI usage (Expand on this)
- AMIable has an infrastructure cost of $40/month or $480 a year. Cloudquery
has no additional cost.
- Doesn't collect information about instances where the bake date is unavailable
- The AMIable UI is inflexible, difficult to contextualise as there is no other
data is available. Important additional information might be "Who do I email
about this out of date AMI?". Cloudquery provides addidional information such
as the account owners contact details, or which repository the instance is
associated with.

#### Reasons to keep Amiable
- AMIable provides near real time information about AMI usage. Cloudquery data
about EC2 instances and AMIs is updated every 24 hours.

### Implementation

We already have a working view that is more complete than the information that
amiable provides. You can see it by running
```sql
select *
from view_old_ec2_instances
```
against the cloudquery database (available as a datasource in grafana). This
information can be grouped, joined and visualised in multiple different ways,
providing a flexible way to interact with the data.

Teams typically take over 24 hours to respond to out of date AMIs, so I don't
see the 24 hour delay in cloudquery as a significant problem. However, if
necessary, we could run the EC2 related tasks more frequently, several times a
day at little additional cost. Running these jobs more frequently might also
take us one step closer to deprecating Prism.

We would also need to delete the AMIable stacks for code and prod, remove its
entry from tools.gutools.co.uk, and archive this repository.

### Status

Pending review
