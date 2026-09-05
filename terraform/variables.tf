variable "service_name" {
  description = "Identifiant de ton projet Public Cloud OVH (sert de tenant_id pour le provider openstack)"
  type        = string
}

variable "openstack_user_name" {
  description = "Nom d'utilisateur OpenStack (créé via Users & Roles, connexion Horizon)"
  type        = string
}

variable "openstack_password" {
  description = "Mot de passe de l'utilisateur OpenStack"
  type        = string
  sensitive   = true
}

variable "instance_name" {
  description = "Nom de l'instance"
  type        = string
  default     = "radmotech-app"
}

variable "flavor_name" {
  description = "Catégorie de l'instance (ex. b3-8)"
  type        = string
}

variable "image_name" {
  description = "Image système à utiliser (ex. Ubuntu 24.04)"
  type        = string
  default     = "Ubuntu 24.04"
}

variable "ssh_public_key" {
  description = "Ta clé publique SSH"
  type        = string
}