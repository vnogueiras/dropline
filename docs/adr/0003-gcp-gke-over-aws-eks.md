# 0003. Deploy on GCP (GKE) instead of AWS (EKS)

- **Status:** Accepted
- **Date:** 2026-11-22
- **Phase / card:** P0-07 (account setup in P0-06, deployment in Phase 7)

## Context
Dropline needs a public cloud deployment to show Kubernetes, managed services and CI/CD working for real. The budget is close to zero: the project is a portfolio piece run by one person, and the cluster only needs to be up for demos, screenshots and interviews.

Free offers at the time of writing (check before relying on them, they change):

| | AWS | GCP |
|---|---|---|
| Sign-up credit | $100, plus up to $100 more for activities; Free plan lasts up to 6 months or until credits run out | $300, valid 90 days |
| Kubernetes control plane | EKS ≈ $0.10/hour ≈ $73/month per cluster, not covered by an always-free tier | GKE free tier: $74.40/month credit per billing account, covers the fee of **one zonal or Autopilot cluster**, permanently |
| Nodes | Paid (EC2) | Paid (Compute Engine), spot VMs available |
| Object storage / functions | S3 / Lambda | Cloud Storage (5 GB always free in US regions) / Cloud Run functions (2M invocations/month free) |

A Kubernetes + Kafka stack is not free for 24/7 operation on either provider. The difference is the control plane: on AWS it consumes most of the credit by itself; on GCP it costs nothing.

## Decision
We will deploy to **GCP**, using a **zonal GKE cluster** (management fee covered by the GKE free tier) with a small **spot node pool**, paid from the $300 credit. All infrastructure is defined in **Terraform** so the environment is created for demos (`terraform apply`) and destroyed afterwards (`terraform destroy`). A €5 billing budget alert is set before anything is created.

GCP-managed services used alongside GKE: Artifact Registry, Cloud Storage, Cloud Run functions, Secret Manager, Cloud Scheduler, Cloud Logging/Monitoring/Trace. Development happens locally (Docker Compose, kind) at no cost.

## Consequences
- ✅ No control-plane cost; the credit goes to nodes, so it lasts the whole 90 days if the cluster is destroyed between demos.
- ✅ Workload Identity (GKE ↔ GCP IAM) and Workload Identity Federation (GitHub Actions → GCP) mean no service-account JSON keys anywhere.
- ✅ The design maps 1:1 to AWS (GKE↔EKS, Cloud Storage↔S3, Cloud Run functions↔Lambda, Secret Manager↔Secrets Manager); a comparison table in the README shows both.
- ⚠️ AWS is more common in job ads; mitigated by documenting the AWS equivalents and keeping everything in portable Kubernetes + Helm.
- ⚠️ The $300 credit expires after 90 days; plan Phase 7 inside that window or sign up close to it.
- ⚠️ Spot nodes can be reclaimed at any time; fine for demos, not for production.
- 🔁 Revisit if the cluster must run permanently (consider Oracle Cloud Always Free ARM + k3s) or if a target employer is AWS-only (port the Terraform to EKS).
